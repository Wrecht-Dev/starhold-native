package dev.starhold.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A media item as returned by the native API (`/api/v1/items`, snake_case).
 *
 * Every field except [id], [type] and [title] is optional: the scanner omits
 * values it does not know, and older rows may predate newer fields.
 */
@Serializable
data class Item(
    val id: String = "",
    val type: String = "unknown",
    val title: String = "Untitled",
    val year: Int? = null,
    val plot: String? = null,
    @kotlinx.serialization.SerialName("runtime_seconds") val runtimeSeconds: Int = 0,
    @kotlinx.serialization.SerialName("parent_id") val parentId: String? = null,
    @kotlinx.serialization.SerialName("season_number") val seasonNumber: Int? = null,
    @kotlinx.serialization.SerialName("episode_number") val episodeNumber: Int? = null,
    val genres: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("sort_title") val sortTitle: String? = null,
    val series: String? = null,
    @kotlinx.serialization.SerialName("series_id") val seriesId: String? = null,
    @kotlinx.serialization.SerialName("series_index") val seriesIndex: Int? = null,
    val franchise: String? = null,
    val issue: Int? = null,
    val artist: String? = null,
    @kotlinx.serialization.SerialName("album_artist") val albumArtist: String? = null,
    @kotlinx.serialization.SerialName("track_number") val trackNumber: Int? = null,
    @kotlinx.serialization.SerialName("disc_number") val discNumber: Int? = null,
    val compilation: Boolean = false,
    @kotlinx.serialization.SerialName("has_image") val hasImage: Boolean = false,
    val tagline: String? = null,
    @kotlinx.serialization.SerialName("official_rating") val officialRating: String? = null,
    val played: Boolean = false,
    val favorite: Boolean = false,
    @kotlinx.serialization.SerialName("position_seconds") val positionSeconds: Int? = null,
) {
    val isPlayable: Boolean
        get() = type in PLAYABLE_TYPES

    val isReadable: Boolean
        get() = type == "book" || type == "comic"

    /** Display credit: album artist first, then artist. */
    val credit: String?
        get() = albumArtist ?: artist

    companion object {
        val PLAYABLE_TYPES = setOf("movie", "episode", "video", "audio", "audiobook")
    }
}

@Serializable
data class Library(val id: String, val name: String, val type: String)

@Serializable
data class HistoryEntry(
    val id: String,
    val title: String,
    val type: String,
    @kotlinx.serialization.SerialName("position_seconds") val positionSeconds: Int = 0,
    val played: Boolean = false,
    @kotlinx.serialization.SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class GenreCount(val name: String, val count: Int = 0)

@Serializable
data class CountResponse(val count: Int = 0)

@Serializable
data class PlaybackState(
    @kotlinx.serialization.SerialName("position_seconds") val positionSeconds: Int = 0,
    val played: Boolean = false,
    val favorite: Boolean = false,
)

@Serializable
data class MediaStream(
    val index: Int = 0,
    val codec: String = "",
    val type: String = "",
    val language: String = "",
    val isDefault: Boolean = false,
    val title: String = "",
    val channels: Int = 0,
    val forced: Boolean = false,
    val external: Boolean = false,
)

@Serializable
data class SubtitleTrack(
    val index: Int = 0,
    val codec: String = "",
    val language: String = "",
    val title: String = "",
    val default: Boolean = false,
    val forced: Boolean = false,
    val external: Boolean = false,
    val url: String = "",
)

@Serializable
data class PlaybackDecision(
    val id: String = "",
    val type: String = "",
    val mode: String = "unsupported",
    val reason: String = "",
    val container: String = "",
    val video: MediaStream = MediaStream(),
    @kotlinx.serialization.SerialName("audio_index") val audioIndex: Int = -1,
    @kotlinx.serialization.SerialName("audio_codec") val audioCodec: String = "",
    @kotlinx.serialization.SerialName("audio_tracks") val audioTracks: List<MediaStream> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    @kotlinx.serialization.SerialName("duration_seconds") val durationSeconds: Int = 0,
    val size: Long = 0,
    val mime: String = "",
    @kotlinx.serialization.SerialName("can_transcode") val canTranscode: Boolean = false,
    @kotlinx.serialization.SerialName("direct_url") val directUrl: String = "",
    @kotlinx.serialization.SerialName("remux_url") val remuxUrl: String = "",
)

@Serializable
data class PlaybackInfo(
    val id: String = "",
    val container: String = "",
    val size: Long = 0,
    val streams: List<MediaStream> = emptyList(),
)

@Serializable
data class Session(
    val token: String,
    val username: String,
    val role: String = "user",
    val canDownload: Boolean = true,
) {
    val isAdmin: Boolean get() = role == "admin"
}

@Serializable
data class MeUser(
    val id: String = "",
    val username: String = "",
    val role: String = "user",
    @kotlinx.serialization.SerialName("can_download") val canDownload: Boolean? = null,
)

@Serializable
data class Capabilities(
    val transcode: String = "",
    val remux: Boolean = false,
    val cpu: Boolean = false,
)

@Serializable
data class ClientCapabilities(
    val containers: List<String> = emptyList(),
    val video: List<String> = emptyList(),
    val audio: List<String> = emptyList(),
)

@Serializable
data class UiPreferences(
    val navPosition: String = "top",
    val density: String = "comfortable",
    val accentColor: String = "",
    val showBackdrops: Boolean = true,
    val showWatchedBadges: Boolean = true,
) {
    companion object {
        val NAV_POSITIONS = listOf("top", "bottom", "left", "right")
        val DENSITIES = listOf("comfortable", "compact")
    }
}

@Serializable
data class UiPreferencesEnvelope(val preferences: UiPreferences = UiPreferences())

@Serializable
data class MfaStatus(val enabled: Boolean = false)

@Serializable
data class MfaBegin(val secret: String = "", val uri: String = "")

@Serializable
data class MfaConfirm(
    val enabled: Boolean = false,
    @kotlinx.serialization.SerialName("recovery_codes") val recoveryCodes: List<String> = emptyList(),
)

@Serializable
data class AppPassword(
    val id: String = "",
    val name: String = "",
    val prefix: String = "",
    @kotlinx.serialization.SerialName("created_at") val createdAt: String = "",
    @kotlinx.serialization.SerialName("last_used_at") val lastUsedAt: String? = null,
    @kotlinx.serialization.SerialName("revoked_at") val revokedAt: String? = null,
)

@Serializable
data class CreatedAppPassword(
    val password: String = "",
    @kotlinx.serialization.SerialName("app_password") val appPassword: AppPassword = AppPassword(),
)

@Serializable
data class CompanionDevice(
    val id: String = "",
    val name: String = "",
    val kind: String? = null,
    @kotlinx.serialization.SerialName("last_seen") val lastSeen: String? = null,
)

@Serializable
data class PairingStart(
    val code: String = "",
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: String = "",
)

@Serializable
data class PairingStatus(
    val status: String = "",
    val device: CompanionDevice? = null,
)

@Serializable
data class LiveChannel(
    val id: String = "",
    val name: String = "",
    val number: Int = 0,
    val logo: String? = null,
    val group: String? = null,
    val favorite: Boolean = false,
    val health: String = "unknown",
)

@Serializable
data class LiveProgramme(
    val id: String = "",
    @kotlinx.serialization.SerialName("channel_id") val channelId: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val description: String? = null,
    @kotlinx.serialization.SerialName("start_at") val startAt: String = "",
    @kotlinx.serialization.SerialName("stop_at") val stopAt: String = "",
    val categories: List<String> = emptyList(),
)

@Serializable
data class EpubInfo(
    val title: String = "",
    val author: String = "",
    val language: String = "",
    val chapters: List<EpubChapter> = emptyList(),
)

@Serializable
data class EpubChapter(val index: Int = 0, val title: String = "")

@Serializable
data class PageInfo(
    val pages: Int = 0,
    val kind: String = "",
    val format: String = "",
)

@Serializable
data class SignupStatus(
    val enabled: Boolean = false,
    val approval: Boolean = false,
    @kotlinx.serialization.SerialName("server_name") val serverName: String? = null,
)

@Serializable
data class InviteValidation(
    val valid: Boolean = false,
    val note: String? = null,
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: String? = null,
    @kotlinx.serialization.SerialName("requires_approval") val requiresApproval: Boolean = false,
)

@Serializable
data class RedeemResult(
    @kotlinx.serialization.SerialName("access_token") val accessToken: String? = null,
    val user: MeUser? = null,
    @kotlinx.serialization.SerialName("pending_approval") val pendingApproval: Boolean = false,
)

/** Jellyfin-shaped item, only the fields the native client reads. */
@Serializable
data class JellyfinItem(
    @kotlinx.serialization.SerialName("Id") val id: String = "",
    @kotlinx.serialization.SerialName("Name") val name: String = "",
    @kotlinx.serialization.SerialName("Type") val type: String = "",
    @kotlinx.serialization.SerialName("ProductionYear") val productionYear: Int? = null,
    @kotlinx.serialization.SerialName("Overview") val overview: String? = null,
    @kotlinx.serialization.SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @kotlinx.serialization.SerialName("ParentId") val parentId: String? = null,
    @kotlinx.serialization.SerialName("SeriesId") val seriesId: String? = null,
    @kotlinx.serialization.SerialName("SeriesName") val seriesName: String? = null,
    @kotlinx.serialization.SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @kotlinx.serialization.SerialName("IndexNumber") val indexNumber: Int? = null,
    @kotlinx.serialization.SerialName("AlbumArtist") val albumArtist: String? = null,
    @kotlinx.serialization.SerialName("Artists") val artists: List<String>? = null,
    @kotlinx.serialization.SerialName("Taglines") val taglines: List<String>? = null,
    @kotlinx.serialization.SerialName("OfficialRating") val officialRating: String? = null,
    @kotlinx.serialization.SerialName("UserData") val userData: JellyfinUserData? = null,
    @kotlinx.serialization.SerialName("HasPrimaryImage") val hasPrimaryImage: Boolean = false,
    @kotlinx.serialization.SerialName("ImageTags") val imageTags: JsonObject? = null,
)

@Serializable
data class JellyfinUserData(
    @kotlinx.serialization.SerialName("Played") val played: Boolean = false,
    @kotlinx.serialization.SerialName("IsFavorite") val favorite: Boolean = false,
    @kotlinx.serialization.SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long? = null,
)

@Serializable
data class JellyfinList(val Items: List<JellyfinItem> = emptyList())

@Serializable
data class JellyfinPlaybackInfoResponse(
    @kotlinx.serialization.SerialName("MediaSources") val mediaSources: List<JellyfinMediaSource> = emptyList(),
)

@Serializable
data class JellyfinMediaSource(
    @kotlinx.serialization.SerialName("Id") val id: String = "",
    @kotlinx.serialization.SerialName("Container") val container: String = "",
    @kotlinx.serialization.SerialName("Size") val size: Long = 0,
    @kotlinx.serialization.SerialName("MediaStreams") val mediaStreams: List<JellyfinMediaStream> = emptyList(),
)

@Serializable
data class JellyfinMediaStream(
    @kotlinx.serialization.SerialName("Index") val index: Int = 0,
    @kotlinx.serialization.SerialName("Codec") val codec: String = "",
    @kotlinx.serialization.SerialName("Type") val type: String = "",
    @kotlinx.serialization.SerialName("Language") val language: String = "",
    @kotlinx.serialization.SerialName("IsDefault") val isDefault: Boolean = false,
)

/** Converts the Jellyfin facade shape into the app's [Item]. */
fun JellyfinItem.toItem(): Item {
    val seconds = ((runTimeTicks ?: 0L) / 10_000_000L).toInt()
    val position = ((userData?.playbackPositionTicks ?: 0L) / 10_000_000L).toInt()
    val mappedType = when (type) {
        "Movie" -> "movie"
        "Series" -> "series"
        "Season" -> "season"
        "Episode" -> "episode"
        "MusicAlbum" -> "album"
        "MusicArtist" -> "artist"
        "Audio" -> "audio"
        "AudioBook" -> "audiobook"
        "Book" -> "book"
        "Video" -> "video"
        else -> type.lowercase()
    }
    return Item(
        id = id,
        type = mappedType,
        title = name.ifBlank { "Untitled" },
        year = productionYear,
        plot = overview,
        runtimeSeconds = seconds,
        parentId = parentId,
        seriesId = seriesId,
        series = seriesName,
        seasonNumber = if (mappedType == "episode") parentIndexNumber else null,
        episodeNumber = if (mappedType == "episode") indexNumber else null,
        trackNumber = if (mappedType == "audio") indexNumber else null,
        discNumber = if (mappedType == "audio") parentIndexNumber else null,
        artist = artists?.firstOrNull(),
        albumArtist = albumArtist,
        played = userData?.played ?: false,
        favorite = userData?.favorite ?: false,
        positionSeconds = position.takeIf { it > 0 },
        hasImage = hasPrimaryImage || !imageTags.isNullOrEmpty(),
        tagline = taglines?.firstOrNull(),
        officialRating = officialRating,
    )
}
