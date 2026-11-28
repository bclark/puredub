package com.puredub.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PureDubScreen(vm: PureDubViewModel, onImport: () -> Unit, onOpen: () -> Unit, onSave: () -> Unit, onExport: () -> Unit, onOpenExport: () -> Unit) {
    val state = vm.state
    val busy = state.busy != null
    var editing by remember { mutableStateOf<Int?>(null) }
    var confirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val guardedReplace: (String, () -> Unit) -> Unit = { description, action ->
        if (state.project.tracks.isNotEmpty() || state.audio != null) confirmation = description to action else action()
    }
    val tabs = listOf("Project", "Tracks", "Export")
    val icons = listOf(R.drawable.ic_project, R.drawable.ic_tracks, R.drawable.ic_export)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) {
            snackbar.showSnackbar(state.message, withDismissAction = true, duration = if (state.error) SnackbarDuration.Indefinite else SnackbarDuration.Long)
            vm.dismissMessage()
        }
    }
    BoxWithConstraints {
        val expanded = maxWidth >= 600.dp
        Scaffold(
            topBar = { TopAppBar(title = { Text("PureDub", style = MaterialTheme.typography.titleLarge) }, actions = {
                TextButton(onClick = onSave, enabled = !busy && state.project.source.isNotBlank()) { Text("Save project") }
            }) },
            bottomBar = { if (!expanded) NavigationBar { tabs.forEachIndexed { index, title ->
                NavigationBarItem(selected = state.tab == index, onClick = { vm.setTab(index) }, icon = { Icon(painterResource(icons[index]), contentDescription = null) }, label = { Text(title) })
            }} },
            snackbarHost = { SnackbarHost(snackbar, snackbar = { data -> Snackbar(data, containerColor = if (state.error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
                contentColor = if (state.error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface) }) }
        ) { insets ->
            Row(Modifier.fillMaxSize().padding(insets).imePadding()) {
                if (expanded) NavigationRail { tabs.forEachIndexed { index, title ->
                    NavigationRailItem(selected = state.tab == index, onClick = { vm.setTab(index) }, icon = { Icon(painterResource(icons[index]), null) }, label = { Text(title) })
                }}
                Column(Modifier.weight(1f).fillMaxHeight().widthIn(max = 1000.dp)) {
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(state.busy, Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
                    }
                    when (state.tab) {
                        0 -> ProjectScreen(state, vm, onImport = { if (state.audio == null) onImport() else guardedReplace("Importing a new recording resets cut positions for the selected side. Save this project first if you want to keep those cuts.", onImport) },
                            onOpen = { guardedReplace("Opening another project replaces the current workbench. Save this project first to keep your edits.", onOpen) },
                            onDemo = { guardedReplace("The synthetic demo replaces the current workbench. Save your project first to keep your edits.", vm::demo) },
                            onNew = { guardedReplace("Starting a new project clears the workbench. Save your project first to keep your edits.", vm::newProject) },
                            onApply = { index -> guardedReplace("Using this edition replaces your track list and cut positions. Save this project first to keep your edits.") { vm.applyMedium(index) } })
                        1 -> TracksScreen(state, vm, onImport, onEdit = { editing = it })
                        2 -> ExportScreen(state, onExport, onOpenExport, onSave)
                    }
                }
            }
        }
    }
    editing?.let { index -> state.project.tracks.getOrNull(index)?.let { track ->
        TrackEditor(index, track, onDismiss = { editing = null }, onSave = { vm.updateTrack(index, it); editing = null }, onRemove = { vm.removeTrack(index); editing = null })
    }}
    confirmation?.let { (description, action) -> AlertDialog(onDismissRequest = { confirmation = null },
        title = { Text("Replace current work?") }, text = { Text(description) },
        confirmButton = { TextButton(onClick = { confirmation = null; action() }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }) }
}

@Composable
private fun ProjectScreen(state: AppState, vm: PureDubViewModel, onImport: () -> Unit, onOpen: () -> Unit, onDemo: () -> Unit, onNew: () -> Unit, onApply: (Int) -> Unit) {
    val enabled = state.busy == null
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.audio == null) {
            Spacer(Modifier.height(12.dp))
            Text("Your tape.\nYour tracks.", style = MaterialTheme.typography.headlineLarge)
            Text("Import a recorded cassette side, review its cuts, and export named, lossless FLAC tracks.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onImport, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Import WAV") }
            OutlinedButton(onClick = onOpen, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Open project") }
            TextButton(onClick = onDemo, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Try synthetic stereo demo") }
            Text("Works offline. Your original recording is never edited.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
        } else {
            SourceSummary(state)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onImport, enabled = enabled) { Text("Replace WAV") }
                TextButton(onClick = onOpen, enabled = enabled) { Text("Open project") }
                TextButton(onClick = onNew, enabled = enabled) { Text("New project") }
            }
        }
        Text("Release details", style = MaterialTheme.typography.titleLarge)
        ProjectField("Artist", state.project.artist, enabled) { value -> vm.editProject { it.copy(artist = value) } }
        ProjectField("Album", state.project.album, enabled) { value -> vm.editProject { it.copy(album = value) } }
        ProjectField("Year / release date", state.project.year, enabled) { value -> vm.editProject { it.copy(year = value) } }
        HorizontalDivider()
        Text("Metadata source", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = !state.metadataOnline, onClick = { vm.setOnline(false) }, label = { Text("Manual / offline") }, enabled = enabled)
            FilterChip(selected = state.metadataOnline, onClick = { vm.setOnline(true) }, label = { Text("MusicBrainz") }, enabled = enabled)
        }
        if (state.metadataOnline) {
            Text("Search terms and release IDs go to MusicBrainz. Audio stays on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = vm::search, enabled = enabled && (state.project.artist.isNotBlank() || state.project.album.isNotBlank()), modifier = Modifier.fillMaxWidth()) { Text("Find cassette editions") }
            state.releases.forEach { release ->
                ListItem(headlineContent = { Text(release.getString("title")) }, supportingContent = { Text(release.optString("artist") + "\n" + release.optString("detail")) },
                    modifier = Modifier.clickable(enabled = enabled) { vm.loadRelease(release.getJSONObject("id").getString("value")) })
            }
            state.release?.let { release ->
                Text("Choose the physical cassette", style = MaterialTheme.typography.titleMedium)
                val media = release.getJSONArray("media")
                for (i in 0 until media.length()) {
                    val medium = media.getJSONObject(i)
                    if (medium.optString("format") == "Cassette") OutlinedButton(onClick = { onApply(i) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text("Cassette ${medium.getInt("position")} · ${medium.getJSONArray("tracks").length()} tracks ${medium.optString("title")}")
                    }
                }
                Text(release.getString("attribution"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else Text("Enter track titles, durations and cuts yourself. A metadata service is optional.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.project.releaseId.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) {
            Text("Metadata: MusicBrainz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { uriHandler.openUri("https://musicbrainz.org/release/${state.project.releaseId}") }, enabled = enabled) { Text("View selected edition") }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SourceSummary(state: AppState) {
    val info = state.audio ?: return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(state.sourceName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text("${info.bits}-bit · ${info.sampleRate / 1000.0} kHz · ${if (info.channels == 2) "Stereo" else "Mono"} · ${formatTime(info.duration)}",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (state.sourceName.startsWith("Synthetic")) Text("Synthetic test audio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    if (info.fullScale > 0) Text("${info.fullScale} full-scale samples. Listen for clipping; this count alone cannot prove it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TracksScreen(state: AppState, vm: PureDubViewModel, onImport: () -> Unit, onEdit: (Int) -> Unit) {
    val enabled = state.busy == null
    LazyColumn(Modifier.fillMaxSize().testTag("tracks-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Side workbench", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("A", "B").forEach { side -> FilterChip(selected = state.project.side == side, enabled = enabled,
                    onClick = { vm.editProject { it.copy(side = side) } }, label = { Text("Side $side") }) }
            }
            Text("Side selects tracks to export; it does not switch the loaded WAV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.audio != null) {
            item { SourceSummary(state) }
            item { Waveform(state, enabled, vm::seek) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { if (state.playing) vm.stopPlayback() else vm.preview() }, enabled = enabled) {
                        Icon(painterResource(if (state.playing) R.drawable.ic_stop else R.drawable.ic_play), null)
                        Spacer(Modifier.width(8.dp)); Text(if (state.playing) "Stop" else "Play 10s")
                    }
                    Text(formatTime(state.cursor), style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                }
                Text("Tap the waveform to seek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = state.cursor.toFloat(), onValueChange = { vm.seek(it.toDouble()) }, valueRange = 0f..state.audio.duration.toFloat().coerceAtLeast(0.001f),
                    enabled = enabled, modifier = Modifier.semantics { contentDescription = "Recording position" })
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(state.project.offset, onValueChange = { value -> vm.editProject { it.copy(offset = value) } }, label = { Text("Leading silence") },
                        singleLine = true, enabled = enabled, isError = parseTime(state.project.offset) == null, modifier = Modifier.weight(1f))
                    OutlinedTextField(state.project.end, onValueChange = { value -> vm.editProject { it.copy(end = value) } }, label = { Text("Side end") },
                        singleLine = true, enabled = enabled, isError = parseTime(state.project.end) == null, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = state.quietSnap, onCheckedChange = vm::setQuiet, enabled = enabled,
                        modifier = Modifier.semantics { contentDescription = "Find nearby quiet within eight seconds" })
                    Text("Find nearby quiet (±8s)", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = vm::suggest, enabled = enabled && state.project.tracks.any { it.side == state.project.side }, modifier = Modifier.fillMaxWidth()) { Text("Suggest cuts from durations") }
            }
        } else item {
            Text("Import a WAV to see its waveform and audition cuts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onImport, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Import WAV") }
        }
        item { HorizontalDivider(); Spacer(Modifier.height(8.dp)); Text("Album tracks", style = MaterialTheme.typography.titleLarge) }
        if (state.project.tracks.isEmpty()) item { Text("Add tracks manually, or choose a cassette edition on the Project tab.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(state.project.tracks) { index, track ->
            val selected = index == state.selected
            Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { vm.select(index) }.semantics { this.selected = selected }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(String.format(Locale.ROOT, "%02d", index + 1), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.weight(1f)) {
                            Text(track.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                            Text("Side ${track.side} · ${track.start.ifBlank { "Start unset" }}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                        }
                        TextButton(onClick = { vm.select(index); onEdit(index) }, enabled = enabled) { Text("Edit") }
                    }
                    if (selected && track.side == state.project.side && state.audio != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = vm::setStart, enabled = enabled) { Text("Set start at cursor") }
                        TextButton(onClick = { vm.preview(true) }, enabled = enabled && parseTime(track.start) != null) { Text("Listen across cut") }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = vm::addTrack, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Add track") } }
        item { Text("Review every cut by listening before export. Unknown sides stay ? until you assign them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Waveform(state: AppState, enabled: Boolean, onSeek: (Double) -> Unit) {
    val audio = state.audio ?: return
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxWidth().height(128.dp).testTag("waveform").semantics { contentDescription = "Waveform with track cuts. Use the recording position slider to seek accessibly." }
        .pointerInput(audio.duration, enabled) { detectTapGestures { offset -> if (enabled) onSeek((offset.x / size.width).coerceIn(0f, 1f) * audio.duration) } }) {
        drawRect(colors.surfaceContainer)
        val middle = size.height / 2
        drawLine(colors.outlineVariant, Offset(0f, middle), Offset(size.width, middle))
        audio.peaks.forEachIndexed { i, peak ->
            val x = i.toFloat() / audio.peaks.size * size.width
            drawLine(colors.secondary, Offset(x, middle - peak * middle * 0.85f), Offset(x, middle + peak * middle * 0.85f), 1.5f)
        }
        state.project.tracks.filter { it.side == state.project.side }.forEach { track -> parseTime(track.start)?.takeIf { it <= audio.duration }?.let { start ->
            val x = (start / audio.duration * size.width).toFloat()
            drawLine(colors.primary, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
        }}
        val cursor = (state.cursor / audio.duration * size.width).toFloat()
        drawLine(colors.onSurface, Offset(cursor, 0f), Offset(cursor, size.height), 2.dp.toPx())
    }
}

@Composable
private fun ExportScreen(state: AppState, onExport: () -> Unit, onOpenExport: () -> Unit, onSave: () -> Unit) {
    val count = state.project.tracks.count { it.side == state.project.side }
    val hasAudio = state.audio != null
    val enabled = state.busy == null
    var validation by remember { mutableStateOf("Checking track ranges…") }
    LaunchedEffect(state.project, state.audio, state.busy) {
        validation = if (!hasAudio) "Import a WAV first."
        else if (count == 0) "Add and assign tracks to Side ${state.project.side}."
        else if (state.busy != null) "Processing…"
        else withContext(Dispatchers.IO) { runCatching { NativeCore.call("validate", "project" to state.project.json()); "" }.getOrElse { it.message ?: "Review track boundaries." } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Ready for your library", style = MaterialTheme.typography.headlineSmall)
        Text(state.project.album.ifBlank { "Untitled album" }, style = MaterialTheme.typography.titleLarge)
        Text("${state.project.artist.ifBlank { "Unknown artist" }} · Side ${state.project.side} · $count tracks", style = MaterialTheme.typography.bodyLarge)
        if (hasAudio) SourceSummary(state)
        if (validation.isNotEmpty()) Text(validation, color = MaterialTheme.colorScheme.error)
        Button(onClick = onExport, enabled = enabled && validation.isEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_export), null); Spacer(Modifier.width(8.dp)); Text("Export FLAC")
        }
        Text("Choose a writable parent folder. A new album/side folder is created. Keep PureDub open until copying finishes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Original samples preserved", style = MaterialTheme.typography.titleMedium)
        Text("Each FLAC is decoded after tagging and compared with your captured PCM. Files are then copied to a new folder and checked again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("No normalization, fades, resampling or noise reduction.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onSave, enabled = enabled && hasAudio, modifier = Modifier.fillMaxWidth()) { Text("Save project separately") }
        if (state.exportResult.isNotBlank()) {
            HorizontalDivider()
            Text("Export complete", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(state.exportResult, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onOpenExport, modifier = Modifier.fillMaxWidth()) { Text("Open export folder") }
        }
    }
}

@Composable
private fun ProjectField(label: String, value: String, enabled: Boolean, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValueChange = onValue, label = { Text(label) }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun TrackEditor(index: Int, track: DubTrack, onDismiss: () -> Unit, onSave: (DubTrack) -> Unit, onRemove: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(track.title) }
    var side by rememberSaveable { mutableStateOf(track.side) }
    var duration by rememberSaveable { mutableStateOf(track.lengthMs?.let { (it / 1000.0).toString() } ?: "") }
    var start by rememberSaveable { mutableStateOf(track.start) }
    val durationValue = duration.toDoubleOrNull()
    val durationValid = duration.isBlank() || (durationValue != null && durationValue.isFinite() && durationValue >= 0.001 && durationValue <= 7200)
    val startValid = start.isBlank() || parseTime(start) != null
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit track ${index + 1}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("A", "B", "?").forEach { value ->
                FilterChip(selected = side == value, onClick = { side = value }, label = { Text(if (value == "?") "Unknown" else "Side $value") })
            }}
            OutlinedTextField(duration, onValueChange = { duration = it }, label = { Text("Duration in seconds (optional)") }, isError = !durationValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text(if (durationValid) "Used for cut suggestions; leave blank if unknown." else "Enter 0.001–7200 seconds, or leave blank.") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(start, onValueChange = { start = it }, label = { Text("Start (m:ss.sss)") }, isError = !startValid, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onRemove) { Text("Remove track", color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = { onSave(DubTrack(title.ifBlank { "Untitled" }, side, durationValue?.takeIf { duration.isNotBlank() }?.let { (it * 1000).roundToLong() }, start)) }, enabled = durationValid && startValid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
