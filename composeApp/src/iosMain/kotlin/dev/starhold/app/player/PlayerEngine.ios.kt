@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.starhold.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import dev.starhold.core.model.ClientCapabilities
import dev.starhold.core.network.MediaRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL

actual fun clientCapabilities(): ClientCapabilities = ClientCapabilities(
    containers = listOf("mp4", "mov", "m4v", "ts", "m3u8"),
    video = listOf("h264", "hevc"),
    audio = listOf("aac", "ac3", "eac3", "alac", "mp3"),
)

actual fun playerEngineName(): String = "AVPlayer (system)"

actual fun drawsInApp(): Boolean = true

/**
 * AVPlayer backed by AVPlayerViewController. Using the controller's own view
 * avoids hand-managed AVPlayerLayer frames and keeps PiP/rotation behaviour
 * consistent with the platform.
 */
class IosPlayerEngine : PlayerEngine {
    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status

    val player: AVPlayer = AVPlayer()
    private var timeObserver: Any? = null
    private var endObserver: Any? = null

    init {
        timeObserver = player.addPeriodicTimeObserverForInterval(
            CMTimeMakeWithSeconds(0.4, 600),
            null,
        ) { _ ->
            val item = player.currentItem
            val duration = item?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            val position = CMTimeGetSeconds(player.currentTime())
            _status.value = _status.value.copy(
                positionMs = (position * 1000).toLong().coerceAtLeast(0L),
                durationMs = if (duration.isFinite() && duration > 0.0) (duration * 1000).toLong() else 0L,
                playing = player.rate > 0f,
            )
        }
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            null,
            null,
        ) { _ ->
            _status.value = _status.value.copy(ended = true, playing = false)
        }
    }

    override fun prepare(request: MediaRequest, startMs: Long) {
        val url = NSURL.URLWithString(request.url) ?: return
        val asset = if (request.headers.isEmpty()) {
            AVURLAsset(uRL = url, options = null)
        } else {
            AVURLAsset(
                uRL = url,
                options = mapOf<Any?, Any?>("AVURLAssetHTTPHeaderFieldsKey" to request.headers),
            )
        }
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(asset = asset))
        _status.value = PlaybackStatus(playing = true)
        if (startMs > 0) {
            player.seekToTime(CMTimeMakeWithSeconds(startMs / 1000.0, 600))
        }
    }

    override fun play() {
        player.play()
        _status.value = _status.value.copy(playing = true, ended = false)
    }

    override fun pause() {
        player.pause()
        _status.value = _status.value.copy(playing = false)
    }

    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 600))
        _status.value = _status.value.copy(positionMs = positionMs, ended = false)
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun setSpeed(rate: Float) {
        player.rate = rate
    }

    override fun release() {
        timeObserver?.let { player.removeTimeObserver(it) }
        timeObserver = null
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }
}

@Composable
actual fun rememberPlayerEngine(): PlayerEngine {
    val engine = remember { IosPlayerEngine() }
    DisposableEffect(engine) { onDispose { engine.release() } }
    return engine
}

@Composable
actual fun PlayerSurface(engine: PlayerEngine, modifier: Modifier) {
    val ios = engine as? IosPlayerEngine ?: return
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                player = ios.player
                showsPlaybackControls = false
                videoGravity = AVLayerVideoGravityResizeAspect
            }
        },
        modifier = modifier,
        update = { controller ->
            (controller as? AVPlayerViewController)?.player = ios.player
        },
    )
}
