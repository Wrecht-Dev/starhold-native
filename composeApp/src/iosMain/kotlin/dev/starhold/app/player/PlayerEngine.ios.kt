package dev.starhold.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import dev.starhold.core.model.ClientCapabilities
import dev.starhold.core.network.MediaRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

actual fun clientCapabilities(): ClientCapabilities = ClientCapabilities(
    containers = listOf("mp4", "mov", "m4v", "ts", "m3u8"),
    video = listOf("h264", "hevc"),
    audio = listOf("aac", "ac3", "eac3", "alac", "mp3"),
)

actual fun playerEngineName(): String = "AVPlayer (system)"

actual fun drawsInApp(): Boolean = true

class IosPlayerEngine : PlayerEngine {
    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status
    val player: AVPlayer = AVPlayer()
    private var observer: Any? = null

    init {
        observer = player.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(0.4, 600),
            queue = null,
        ) { _ ->
            val item = player.currentItem
            val duration = item?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            val position = CMTimeGetSeconds(player.currentTime())
            _status.value = _status.value.copy(
                positionMs = (position * 1000).toLong().coerceAtLeast(0L),
                durationMs = if (duration.isFinite()) (duration * 1000).toLong() else 0L,
                playing = player.timeControlStatus == AVPlayerTimeControlStatusPlaying,
            )
        }
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = null,
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
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(asset))
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
        observer?.let { player.removeTimeObserver(it) }
        observer = null
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
    val layer = remember {
        AVPlayerLayer().apply {
            player = ios.player
            videoGravity = AVLayerVideoGravityResizeAspect
            backgroundColor = null
        }
    }
    UIKitView(
        factory = {
            UIView().apply {
                layer.addSublayer(layer)
            }
        },
        modifier = modifier,
        update = { view ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            layer.frame = view.bounds
            CATransaction.commit()
        },
    )
}
