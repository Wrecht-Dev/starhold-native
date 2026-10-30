package dev.starhold.core.format

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max

/** "1h 23m", "45m", "38s"; empty for unknown durations. */
fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/** Player clock: "1:23:45" or "23:45". */
fun formatClock(seconds: Int): String {
    val safe = max(0, seconds)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return if (h > 0) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "$m:${s.toString().padStart(2, '0')}"
    }
}

/** Human label for a media type, matching the web client's vocabulary. */
fun typeLabel(type: String): String = when (type) {
    "movie" -> "Movie"
    "series" -> "Series"
    "season" -> "Season"
    "episode" -> "Episode"
    "album" -> "Album"
    "artist" -> "Artist"
    "audio" -> "Track"
    "audiobook" -> "Audiobook"
    "book" -> "Book"
    "comic" -> "Comic"
    "video" -> "Video"
    "game" -> "Game"
    "podcast" -> "Podcast"
    "podcastepisode" -> "Episode"
    else -> type.replaceFirstChar { it.uppercase() }
}

/** 2:3 poster vs 1:1 square vs 16:9 still. */
fun isSquareArt(type: String): Boolean = type == "audio" || type == "album" || type == "artist"

/** Episode caption "S1 E2 · 42m"; album caption "Artist · 2019"; else year. */
fun subtitleFor(
    type: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    credit: String?,
    year: Int?,
    runtimeSeconds: Int,
): String? = when {
    type == "episode" && seasonNumber != null && episodeNumber != null ->
        "S$seasonNumber E$episodeNumber"
    type == "episode" && runtimeSeconds > 0 -> formatDuration(runtimeSeconds)
    credit != null && year != null -> "$credit · $year"
    credit != null -> credit
    year != null -> year.toString()
    runtimeSeconds > 0 -> formatDuration(runtimeSeconds)
    else -> null
}

/** Relative time for history rows, tolerant of absent/invalid timestamps. */
fun relativeTime(iso: String, now: Instant = Clock.System.now()): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val minutes = (now - instant).inWholeMinutes
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)}d ago"
        else -> "${minutes / (60 * 24 * 30)}mo ago"
    }
}

/** "Ends at 21:40" for a resume-aware detail page. */
fun endsAt(now: Instant, remainingSeconds: Int, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val end = Instant.fromEpochSeconds(now.epochSeconds + remainingSeconds)
    val local = end.toLocalDateTime(zone)
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "Ends at $hour:$minute"
}

/** "1 season" / "3 seasons". */
fun plural(count: Int, singular: String, plural: String): String =
    "$count " + if (count == 1) singular else plural
