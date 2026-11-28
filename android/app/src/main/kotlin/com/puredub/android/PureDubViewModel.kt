package com.puredub.android

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

data class AppState(
    val project: DubProject = DubProject(), val audio: AudioInfo? = null,
    val sourceName: String = "", val sourceUri: String = "", val cursor: Double = 0.0,
    val selected: Int = 0, val busy: String? = null, val message: String = "",
    val error: Boolean = false, val playing: Boolean = false, val tab: Int = 0,
    val quietSnap: Boolean = true, val metadataOnline: Boolean = false,
    val releases: List<JSONObject> = emptyList(), val release: JSONObject? = null,
    val exportResult: String = "", val exportFolder: String = ""
)

class PureDubViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(AppState())
        private set
    private val app = application
    private val resolver = app.contentResolver
    private val sessionFile = File(app.filesDir, "session.json")
    private val sourceNames = app.getSharedPreferences("source-names", Context.MODE_PRIVATE)
    @Volatile private var playback: AudioTrack? = null
    private var playJob: Job? = null
    private var persistJob: Job? = null
    private val saveMutex = Mutex()

    init {
        if (sessionFile.exists()) task("Restoring project…") {
            val saved = withContext(Dispatchers.IO) { JSONObject(sessionFile.readText()) }
            val project = DubProject.fromJson(saved.getJSONObject("project"))
            val info = withContext(Dispatchers.IO) {
                check(File(project.source).exists()) { "The cached source is missing. Reimport the WAV to relink this project." }
                AudioInfo.fromJson(NativeCore.call("load", "path" to project.source, "budget" to memoryBudget()) as JSONObject)
            }
            state = state.copy(project = project, audio = info, sourceName = saved.optString("source_name"), sourceUri = saved.optString("source_uri"))
        }
    }

    private fun memoryBudget(): Long {
        val memory = ActivityManager.MemoryInfo()
        (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        return (memory.availMem / 3).coerceIn(64L * 1_048_576, 768L * 1_048_576)
    }

    private fun task(label: String, action: suspend () -> Unit) {
        if (state.busy != null) return
        stopPlayback()
        state = state.copy(busy = label, message = "", error = false)
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { state = state.copy(message = e.message ?: "Operation failed. Try again.", error = true) }
            finally { state = state.copy(busy = null) }
        }
    }

    fun dismissMessage() { state = state.copy(message = "") }
    fun setTab(tab: Int) { state = state.copy(tab = tab) }
    fun select(index: Int) { state = state.copy(selected = index) }
    fun seek(seconds: Double) { stopPlayback(); state = state.copy(cursor = seconds.coerceIn(0.0, state.audio?.duration ?: 0.0)) }
    fun setQuiet(value: Boolean) { state = state.copy(quietSnap = value) }
    fun setOnline(value: Boolean) { state = state.copy(metadataOnline = value, releases = emptyList(), release = null) }

    fun newProject() = task("Starting a new project…") {
        persistJob?.cancel()
        withContext(Dispatchers.IO) {
            saveMutex.withLock { android.util.AtomicFile(sessionFile).delete() }
            NativeCore.call("clear")
        }
        relinkPending = false
        state = AppState(busy = state.busy)
    }

    fun editProject(update: (DubProject) -> DubProject) {
        if (state.busy != null) return
        stopPlayback()
        state = state.copy(project = update(state.project), exportResult = "")
        persist()
    }

    private fun persist() {
        if (state.project.source.isBlank()) return
        val saved = JSONObject().put("project", state.project.json()).put("source_name", state.sourceName).put("source_uri", state.sourceUri).toString()
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            try { withContext(Dispatchers.IO) { saveMutex.withLock {
                currentCoroutineContext().ensureActive()
                // Atomic replacement prevents a partially written autosave after interruption.
                val file = android.util.AtomicFile(sessionFile)
                val stream = file.startWrite()
                try { stream.write(saved.toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
                catch (e: Exception) { file.failWrite(stream); throw e }
            } } } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state = state.copy(message = "Autosave failed: ${e.message}. Save the project to a document.", error = true) }
        }
    }

    private fun nameOf(uri: Uri): String = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "Recording.wav"

    fun importWav(uri: Uri) = task("Copying and reading WAV…") {
        val imported = withContext(Dispatchers.IO) {
            runCatching { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val source = File(app.filesDir, "sources/${UUID.randomUUID()}.wav").also { it.parentFile!!.mkdirs() }
            try {
                resolver.openInputStream(uri)?.use { input -> source.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= 2_000_000_000) { "Import supports WAV files up to 2 GB." }
                        output.write(buffer, 0, count)
                    }
                }} ?: error("Could not open this WAV. Choose a locally available file.")
                Triple(source, nameOf(uri), AudioInfo.fromJson(NativeCore.call("load", "path" to source.absolutePath, "budget" to memoryBudget()) as JSONObject))
            } catch (e: Exception) { source.delete(); throw e }
        }
        val (source, name, info) = imported
        sourceNames.edit().putString(source.absolutePath, name).apply()
        val project = state.project.copy(source = source.absolutePath, end = if (relinkPending) state.project.end else formatTime(info.duration),
            tracks = if (relinkPending) state.project.tracks else state.project.tracks.map { if (it.side == state.project.side) it.copy(start = "") else it })
        relinkPending = false
        state = state.copy(project = project, audio = info, sourceName = name, sourceUri = uri.toString(), cursor = 0.0,
            message = "WAV imported. Your original remains untouched.", exportResult = "")
        persist()
    }

    fun demo() = task("Preparing synthetic stereo demo…") {
        val source = File(app.filesDir, "sources/${UUID.randomUUID()}.wav").also { it.parentFile!!.mkdirs() }
        val info = withContext(Dispatchers.IO) { AudioInfo.fromJson(NativeCore.call("demo", "path" to source.absolutePath) as JSONObject) }
        sourceNames.edit().putString(source.absolutePath, "Synthetic stereo demo.wav").apply()
        state = state.copy(project = DubProject(source = source.absolutePath, artist = "Synthetic test", album = "Stereo demo", end = "0:08.000",
            tracks = listOf(DubTrack("First tone", "A", 4000, "0:00.000"), DubTrack("Second tone", "A", null, "0:04.000"))),
            audio = info, sourceName = "Synthetic stereo demo.wav", sourceUri = "", cursor = 0.0, selected = 0,
            message = "Synthetic audio: left 440 Hz, right 660 Hz. Not a cassette recording.", exportResult = "")
        relinkPending = false
        persist()
    }

    fun addTrack() {
        editProject { it.copy(tracks = it.tracks + DubTrack(side = it.side, start = formatTime(state.cursor))) }
        state = state.copy(selected = state.project.tracks.lastIndex, tab = 1)
    }
    fun updateTrack(index: Int, track: DubTrack) = editProject { p -> p.copy(tracks = p.tracks.mapIndexed { i, t -> if (i == index) track else t }) }
    fun removeTrack(index: Int) {
        editProject { p -> p.copy(tracks = p.tracks.filterIndexed { i, _ -> i != index }) }
        state = state.copy(selected = state.selected.coerceAtMost((state.project.tracks.size - 1).coerceAtLeast(0)))
    }
    fun setStart() {
        val index = state.selected
        state.project.tracks.getOrNull(index)?.let { updateTrack(index, it.copy(start = formatTime(state.cursor))) }
    }
    fun suggest() = task("Suggesting track starts…") {
        val result = withContext(Dispatchers.IO) { NativeCore.call("suggest", "project" to state.project.json(), "quiet" to state.quietSnap) as JSONObject }
        state = state.copy(project = DubProject.fromJson(result), message = "Suggested cuts are ready. Listen across each cut before exporting.")
        persist()
    }

    fun saveProject(uri: Uri) = task("Saving project…") {
        val project = state.project
        val sourceName = state.sourceName
        withContext(Dispatchers.IO) {
            val data = NativeCore.call("project", "project" to project.json()) as JSONObject
            data.put("android_source_name", sourceName)
            resolver.openOutputStream(uri, "w")?.use { it.write(data.toString(2).toByteArray(Charsets.UTF_8)) } ?: error("Cannot write the selected project document.")
        }
        state = state.copy(message = "Project saved. It references this device’s cached WAV; keep the original recording too.")
    }

    fun openProject(uri: Uri) = task("Opening project…") {
        val (project, savedName) = withContext(Dispatchers.IO) {
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) { val count = input.read(buffer); if (count < 0) break; check(output.size() + count <= 2_000_000) { "Project JSON is too large." }; output.write(buffer, 0, count) }
                output.toByteArray()
            } ?: error("Cannot open this project.")
            check(bytes.size <= 2_000_000) { "Project JSON is too large." }
            val raw = JSONObject(bytes.toString(Charsets.UTF_8))
            DubProject.fromJson(NativeCore.call("project", "project" to raw) as JSONObject) to raw.optString("android_source_name")
        }
        if (File(project.source).exists() && File(project.source).canonicalPath.startsWith(File(app.filesDir, "sources").canonicalPath + File.separator)) {
            val info = withContext(Dispatchers.IO) { AudioInfo.fromJson(NativeCore.call("load", "path" to project.source, "budget" to memoryBudget()) as JSONObject) }
            val name = savedName.ifBlank { sourceNames.getString(project.source, null) ?: File(project.source).name }
            state = state.copy(project = project, audio = info, sourceName = name, sourceUri = "", cursor = 0.0, selected = 0, message = "Project opened.")
            persist()
            relinkPending = false
        } else {
            withContext(Dispatchers.IO) { NativeCore.call("clear") }
            state = state.copy(project = project.copy(source = ""), audio = null, sourceName = "", sourceUri = "", cursor = 0.0, selected = 0,
                message = "Project loaded. Import its original WAV to relink the source; reviewed cut times will be retained.")
            relinkPending = true
        }
    }
    private var relinkPending = false

    fun search() = task("Searching MusicBrainz…") {
        check(state.metadataOnline) { "Select MusicBrainz to search online." }
        val result = withContext(Dispatchers.IO) { NativeCore.call("search", "artist" to state.project.artist, "album" to state.project.album) as JSONArray }
        state = state.copy(releases = (0 until result.length()).map { result.getJSONObject(it) }, release = null,
            message = if (result.length() == 0) "No cassette editions found. Try different terms or enter tracks manually." else "Choose the exact cassette edition.")
    }
    fun loadRelease(id: String) = task("Loading cassette edition…") {
        val release = withContext(Dispatchers.IO) { NativeCore.call("release", "id" to id) as JSONObject }
        state = state.copy(release = release)
    }
    fun applyMedium(index: Int) {
        val release = state.release ?: return
        val medium = release.getJSONArray("media").getJSONObject(index)
        val tracks = medium.getJSONArray("tracks")
        editProject { p -> p.copy(
            artist = release.optString("artist").ifBlank { p.artist }, album = release.optString("title").ifBlank { p.album },
            year = if (release.isNull("date")) "" else release.getString("date"), releaseId = release.getJSONObject("id").getString("value"),
            tracks = (0 until tracks.length()).map { i -> val t = tracks.getJSONObject(i)
                DubTrack(t.getString("title"), t.optString("side").takeIf { it == "A" || it == "B" } ?: "?",
                    if (t.isNull("length_ms")) null else t.getLong("length_ms"), "")
            }) }
        state = state.copy(selected = 0, tab = 1, releases = emptyList(), release = null,
            message = "Track list loaded. Confirm titles, side assignments and order against the sleeve.")
    }

    fun preview(acrossCut: Boolean = false) {
        if (state.busy != null || state.audio == null) return
        stopPlayback()
        val cut = if (acrossCut) state.project.tracks.getOrNull(state.selected)?.start?.let(::parseTime) else state.cursor
        if (cut == null) { state = state.copy(message = "Enter a valid start time first.", error = true); return }
        val start = if (acrossCut) (cut - 3.0).coerceAtLeast(0.0) else cut
        val source = File(app.cacheDir, "preview-${UUID.randomUUID()}.pcm")
        state = state.copy(playing = true, message = "", cursor = start)
        playJob = viewModelScope.launch {
            val currentJob = currentCoroutineContext()[Job]!!
            try {
                withContext(Dispatchers.IO) {
                    val info = NativeCore.call("preview", "path" to source.absolutePath, "start" to start, "seconds" to if (acrossCut) 6.0 else 10.0) as JSONObject
                    currentCoroutineContext().ensureActive()
                    val channels = info.getInt("channels")
                    val channelMask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
                    val rate = info.getInt("sample_rate")
                    val minBuffer = AudioTrack.getMinBufferSize(rate, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
                    check(minBuffer > 0) { "This device cannot preview this sample rate. Export preserves the source format." }
                    val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(rate).setChannelMask(channelMask).build())
                        .setBufferSizeInBytes(maxOf(minBuffer, 32768)).setTransferMode(AudioTrack.MODE_STREAM).build()
                    playback = track
                    try {
                        check(track.state == AudioTrack.STATE_INITIALIZED) { "Cannot open audio playback." }
                        track.play()
                        val buffer = ByteBuffer.allocateDirect(32768)
                        source.inputStream().channel.use { input ->
                            while (currentJob.isActive) {
                                buffer.clear()
                                val count = input.read(buffer)
                                if (count < 0) break
                                buffer.flip()
                                while (buffer.hasRemaining() && currentJob.isActive) {
                                    check(track.write(buffer, buffer.remaining(), AudioTrack.WRITE_BLOCKING) > 0) { "Audio playback stopped unexpectedly." }
                                    withContext(Dispatchers.Main) { state = state.copy(cursor = start + track.playbackHeadPosition.toDouble() / rate) }
                                }
                            }
                        }
                        val totalFrames = (source.length() / (channels * 4)).toInt()
                        while (currentJob.isActive && track.playbackHeadPosition < totalFrames) {
                            withContext(Dispatchers.Main) { state = state.copy(cursor = start + track.playbackHeadPosition.toDouble() / rate) }
                            Thread.sleep(30)
                        }
                        if (currentJob.isActive) withContext(Dispatchers.Main) { state = state.copy(cursor = start + info.getDouble("duration")) }
                    } finally { if (playback === track) playback = null; track.release() }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state = state.copy(message = e.message ?: "Playback failed.", error = true) }
            finally { source.delete(); if (playJob === currentJob) { playJob = null; state = state.copy(playing = false) } }
        }
    }

    fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        runCatching { playback?.pause(); playback?.flush() }
        state = state.copy(playing = false)
    }

    fun exportTo(tree: Uri) = task("Encoding and verifying FLAC tracks… Keep PureDub open.") {
        val project = state.project
        val working = File(app.filesDir, "exports/${UUID.randomUUID()}").also { it.mkdirs() }
        var localFolder: File? = null
        try {
            val result = withContext(Dispatchers.IO) { NativeCore.call("export", "project" to project.json(), "parent" to working.absolutePath) as JSONObject }
            val verifiedFolder = File(result.getString("folder"))
            localFolder = verifiedFolder
            state = state.copy(busy = "Copying verified tracks to your chosen folder…")
            val folderName = verifiedFolder.name
            val destination = withContext(Dispatchers.IO) {
                val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) check(cursor.getString(0) != folderName) { "An export folder with this name already exists. Choose another destination or change the album name." }
                } ?: error("Cannot inspect the export destination.")
                val folder = DocumentsContract.createDocument(resolver, root, DocumentsContract.Document.MIME_TYPE_DIR, folderName) ?: error("Cannot create a new export folder.")
                val exportedFiles = verifiedFolder.listFiles() ?: error("Cannot read the verified export folder.")
                check(exportedFiles.isNotEmpty()) { "The verified export folder is empty." }
                for (file in exportedFiles.sortedBy { it.name }) {
                    val mime = if (file.extension == "flac") "audio/flac" else "application/json"
                    val document = DocumentsContract.createDocument(resolver, folder, mime, file.name) ?: error("Cannot create ${file.name}. Partial export: $folder")
                    val expected = MessageDigest.getInstance("SHA-256")
                    resolver.openOutputStream(document, "w")?.use { output -> file.inputStream().use { input ->
                        val bytes = ByteArray(65536)
                        while (true) { val count = input.read(bytes); if (count < 0) break; expected.update(bytes, 0, count); output.write(bytes, 0, count) }
                    }} ?: error("Cannot copy ${file.name}. Partial export: $folder")
                    val actual = MessageDigest.getInstance("SHA-256")
                    resolver.openInputStream(document)?.use { input ->
                        val bytes = ByteArray(65536)
                        while (true) { val count = input.read(bytes); if (count < 0) break; actual.update(bytes, 0, count) }
                    } ?: error("Cannot verify the copied ${file.name}.")
                    check(expected.digest().contentEquals(actual.digest())) { "Copied-file verification failed for ${file.name}. Partial export: $folder" }
                }
                folder.toString()
            }
            state = state.copy(exportResult = "${result.getInt("tracks")} FLAC tracks exported. PCM samples and copied files verified.", exportFolder = destination,
                message = "Export complete. Original recording unchanged.")
            withContext(Dispatchers.IO) { working.deleteRecursively() }
        } catch (e: Exception) {
            throw IllegalStateException("${e.message} Verified or partial local output is retained in ${localFolder ?: working}. Your original is unchanged.", e)
        }
    }

    override fun onCleared() { stopPlayback(); super.onCleared() }
}
