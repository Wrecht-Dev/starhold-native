package dev.starhold.core.playback

/** One timed subtitle cue in absolute media time. */
data class Cue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Minimal WebVTT parser sufficient for server-generated subtitle tracks:
 * timestamped cues, optional cue identifiers/settings, inline markup stripped.
 * A remux that starts mid-file restarts at zero, so [shiftMs] moves cues back
 * onto the original timeline.
 */
object WebVtt {
    private val timing = Regex(
        """^\s*((?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{3})\s*-->\s*((?:\d{1,2}:)?\d{1,2}:\d{2}[.,]\d{3})""",
    )

    fun parse(body: String, shiftMs: Long = 0): List<Cue> {
        val normalized = body.replace("\r\n", "\n").replace('\r', '\n')
        val blocks = normalized.split(Regex("\n{2,}"))
        val cues = mutableListOf<Cue>()
        for (block in blocks) {
            val lines = block.trim('\n').lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            if (lines.first().trim().startsWith("WEBVTT")) continue
            if (lines.first().trim().startsWith("NOTE")) continue
            val timingIndex = lines.indexOfFirst { timing.containsMatchIn(it) }
            if (timingIndex < 0) continue
            val match = timing.find(lines[timingIndex]) ?: continue
            val start = parseTimestamp(match.groupValues[1]) + shiftMs
            val end = parseTimestamp(match.groupValues[2]) + shiftMs
            val text = lines.drop(timingIndex + 1).joinToString("\n").cleanMarkup()
            if (text.isBlank()) continue
            if (end <= 0) continue
            cues += Cue(start, end, text)
        }
        return cues.sortedBy { it.startMs }
    }

    fun activeAt(cues: List<Cue>, positionMs: Long): Cue? =
        cues.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }

    private fun parseTimestamp(value: String): Long {
        val normalized = value.replace(',', '.')
        val parts = normalized.split(':')
        val (hours, minutes, secondsPart) = when (parts.size) {
            3 -> Triple(parts[0].toInt(), parts[1].toInt(), parts[2])
            2 -> Triple(0, parts[0].toInt(), parts[1])
            else -> return 0
        }
        val seconds = secondsPart.substringBefore('.').toIntOrNull() ?: 0
        val millis = secondsPart.substringAfter('.', "0").padEnd(3, '0').take(3).toIntOrNull() ?: 0
        return ((hours * 60L + minutes) * 60L + seconds) * 1000L + millis
    }

    private fun String.cleanMarkup(): String =
        replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
}
