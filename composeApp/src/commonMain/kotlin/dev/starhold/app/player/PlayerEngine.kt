package dev.starhold.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.starhold.core.model.ClientCapabilities
import dev.starhold.core.network.MediaRequest
import kotlinx.coroutines.flow.StateFlow

/** Live player values the controls and progress sync read. */
data class PlaybackStatus(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val ended: Boolean = false,
    val error: String? = null,
)

/**
 * One engine per platform. Buffering, seeking and track switching are engine
 * concerns; subtitle rendering, progress sync and chrome are shared Compose.
 */
interface PlayerEngine {
    val status: StateFlow<PlaybackStatus>

    fun prepare(request: MediaRequest, startMs: Long)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setSpeed(rate: Float) {}
    fun release()
}

/** Capabilities this device advertises to the server's playback decision. */
expect fun clientCapabilities(): ClientCapabilities

/** Human-readable engine name for the settings/diagnostics screen. */
expect fun playerEngineName(): String

/** True when the engine draws inside the app (Android/iOS); false for mpv. */
expect fun drawsInApp(): Boolean

@Composable
expect fun rememberPlayerEngine(): PlayerEngine

/** Video output. On desktop this is a placeholder while mpv owns its own window. */
@Composable
expect fun PlayerSurface(engine: PlayerEngine, modifier: Modifier)
