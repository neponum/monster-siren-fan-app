package com.example.data.repository

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LrcParser {

    private val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(rawLrc: String?): List<LyricLine> {
        if (rawLrc.isNullOrBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()

        rawLrc.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val matches = timeRegex.findAll(trimmed).toList()
            if (matches.isEmpty()) return@forEach

            val lyricText = timeRegex.replace(trimmed, "").trim()
            if (lyricText.isEmpty()) return@forEach

            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[3]
                val millis = when (fractionRaw.length) {
                    1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fractionRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalMs = minutes * 60_000L + seconds * 1_000L + millis
                lines.add(LyricLine(timestampMs = totalMs, text = lyricText))
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    fun findActiveLineIndex(lyrics: List<LyricLine>, positionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        var active = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timestampMs <= positionMs + 150L) {
                active = i
            } else {
                break
            }
        }
        return active
    }
}
