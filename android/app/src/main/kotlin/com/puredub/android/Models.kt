package com.puredub.android

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToLong

data class DubTrack(val title: String = "Untitled", val side: String = "A", val lengthMs: Long? = null, val start: String = "0:00.000") {
    fun json() = JSONObject().put("title", title).put("side", side)
        .put("length_ms", lengthMs ?: JSONObject.NULL).put("start", start)
}

data class DubProject(
    val source: String = "", val artist: String = "", val album: String = "", val year: String = "",
    val releaseId: String = "", val side: String = "A", val offset: String = "0:00.000",
    val end: String = "", val tracks: List<DubTrack> = emptyList()
) {
    fun json() = JSONObject().put("version", 1).put("source", source).put("artist", artist)
        .put("album", album).put("year", year).put("release_id", releaseId).put("side", side)
        .put("offset", offset).put("end", end).put("tracks", JSONArray(tracks.map { it.json() }))

    companion object {
        fun fromJson(v: JSONObject): DubProject {
            require(v.getInt("version") == 1) { "Unsupported project version." }
            val tracks = v.getJSONArray("tracks")
            return DubProject(v.getString("source"), v.getString("artist"), v.getString("album"),
                v.getString("year"), v.getString("release_id"), v.getString("side"),
                v.getString("offset"), v.getString("end"), (0 until tracks.length()).map { i ->
                    val t = tracks.getJSONObject(i)
                    DubTrack(t.getString("title"), t.getString("side"),
                        if (t.isNull("length_ms")) null else t.getLong("length_ms"), t.getString("start"))
                })
        }
    }
}

data class AudioInfo(val duration: Double, val sampleRate: Int, val bits: Int, val channels: Int, val peaks: List<Float>, val fullScale: Long) {
    companion object {
        fun fromJson(v: JSONObject): AudioInfo {
            val peaks = v.getJSONArray("peaks")
            return AudioInfo(v.getDouble("duration"), v.getInt("sample_rate"), v.getInt("bits"),
                v.getInt("channels"), (0 until peaks.length()).map { peaks.getDouble(it).toFloat() }, v.getLong("full_scale_samples"))
        }
    }
}

fun formatTime(seconds: Double): String {
    val millis = (seconds.coerceAtLeast(0.0) * 1000).roundToLong()
    return String.format(Locale.ROOT, "%d:%02d.%03d", millis / 60000, (millis / 1000) % 60, millis % 1000)
}

fun parseTime(value: String): Double? {
    val parts = value.trim().split(':')
    if (parts.size !in 1..3) return null
    var total = 0.0
    for ((i, part) in parts.withIndex()) {
        val number = part.toDoubleOrNull() ?: return null
        if (!number.isFinite() || number < 0 || (i > 0 && number >= 60) || (i < parts.lastIndex && number % 1.0 != 0.0)) return null
        total = total * 60 + number
    }
    return total.takeIf { it.isFinite() }
}

object NativeCore {
    init { System.loadLibrary("puredub_android") }
    private external fun execute(request: String): String

    fun call(op: String, vararg fields: Pair<String, Any>): Any {
        val request = JSONObject().put("op", op)
        fields.forEach { (key, value) -> request.put(key, value) }
        val response = JSONObject(execute(request.toString()))
        check(response.getBoolean("ok")) { response.optString("error", "Audio operation failed.") }
        return response.get("data")
    }
}
