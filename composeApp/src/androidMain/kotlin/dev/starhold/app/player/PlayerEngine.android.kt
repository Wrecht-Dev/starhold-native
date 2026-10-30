package dev.starhold.app.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.starhold.app.platform.AndroidPlatform
import dev.starhold.core.model.ClientCapabilities
import dev.starhold.core.network.MediaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

actual fun clientCapabilities(): ClientCapabilities = ClientCapabilities(
    containers = listOf("mp4", "mkv", "webm", "ts", "mov", "m4v", "mp3", "m4a", "flac", "aac", "ogg", "opus"),
    video = listOf("h264", "hevc", "vp9", "av1", "mpeg2video", "vc1"),
    audio = listOf("aac", "ac3", "eac3", "mp3", "flac", "opus", "vorbis", "dts", "truehd"),
)

actual fun playerEngineName(): String = "AndroidX Media3 (ExoPlayer)"

actual fun drawsInApp(): Boolean = true

class AndroidPlayerEngine(context: Context) : PlayerEngine {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status
    private var pollJob: Job? = null

    private val httpFactory: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent("StarholdNative/0.1.0 (Android)")

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
        .build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _status.value = _status.value.copy(ended = true, playing = false)
                }
                publish()
            }

            override fun onPlayerError(error: PlaybackException) {
                _status.value = _status.value.copy(error = error.message ?: "Playback failed")
            }
        })
    }

    private fun publish() {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _status.value = _status.value.copy(
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            playing = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            ended = player.playbackState == Player.STATE_ENDED,
        )
    }

    override fun prepare(request: MediaRequest, startMs: Long) {
        httpFactory.setDefaultRequestProperties(request.headers.toMap())
        player.setMediaItem(MediaItem.fromUri(request.url))
        _status.value = PlaybackStatus(playing = true)
        player.prepare()
        if (startMs > 0) player.seekTo(startMs)
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                publish()
                delay(400)
            }
        }
    }

    override fun play() {
        player.play()
        publish()
    }

    override fun pause() {
        player.pause()
        publish()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        publish()
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun setSpeed(rate: Float) {
        player.playbackParameters = PlaybackParameters(rate)
    }

    override fun release() {
        pollJob?.cancel()
        scope.cancel()
        player.release()
    }
}

@Composable
actual fun rememberPlayerEngine(): PlayerEngine {
    val context = AndroidPlatform.appContext
    val engine = remember { AndroidPlayerEngine(context ?: error("Android context not initialised")) }
    DisposableEffect(engine) { onDispose { engine.release() } }
    return engine
}

@Composable
actual fun PlayerSurface(engine: PlayerEngine, modifier: Modifier) {
    val android = engine as? AndroidPlayerEngine ?: return
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                player = android.player
            }
        },
        update = { view -> view.player = android.player },
    )
}
