package dev.starhold.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.core.model.ClientCapabilities
import dev.starhold.core.network.MediaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

actual fun clientCapabilities(): ClientCapabilities = ClientCapabilities(
    containers = listOf("mp4", "mkv", "webm", "mov", "m4v", "ts", "mp3", "flac", "aac", "m4a", "ogg", "opus", "wav"),
    video = listOf("h264", "hevc", "vp9", "av1", "mpeg2video", "vc1", "mpeg4"),
    audio = listOf("aac", "ac3", "eac3", "dts", "truehd", "flac", "mp3", "opus", "vorbis", "alac", "pcm_s16le"),
)

actual fun playerEngineName(): String = "mpv (external window, JSON IPC)"

actual fun drawsInApp(): Boolean = false

/**
 * Desktop playback uses mpv because libmpv embedding needs per-OS GPU interop.
 * mpv owns the video window; this engine owns controls and progress through the
 * JSON IPC socket (Unix domain sockets; Windows launches mpv without IPC).
 */
class MpvPlayerEngine : PlayerEngine {
    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    private var channel: SocketChannel? = null
    private var reader: BufferedReader? = null
    private var socketPath: String? = null
    private var released = false
    private val json = Json { ignoreUnknownKeys = true }

    private val isWindows: Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    override fun prepare(request: MediaRequest, startMs: Long) {
        teardown()
        released = false
        val socket = if (isWindows) null else "/tmp/starhold-mpv-${System.nanoTime()}.sock"
        socketPath = socket
        val args = mutableListOf(
            mpvBinary(),
            "--no-terminal",
            "--really-quiet",
            "--force-window=yes",
            "--keep-open=no",
            "--sid=no",
            "--geometry=80%",
            "--user-agent=StarholdNative/0.1.0",
            "--start=${startMs / 1000.0}",
        )
        if (socket != null) args += "--input-ipc-server=$socket"
        request.headers.forEach { (key, value) -> args += "--http-header-fields=$key: $value" }
        args += request.url

        val started = runCatching {
            ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }.getOrElse { error ->
            _status.value = PlaybackStatus(
                error = "Could not start mpv (${error.message}). Install mpv or set STARHOLD_MPV to its path.",
            )
            return
        }
        process = started
        _status.value = PlaybackStatus(playing = true, buffering = true)

        scope.launch {
            val code = runCatching { started.waitFor() }.getOrDefault(-1)
            if (!released) {
                _status.value = _status.value.copy(
                    playing = false,
                    ended = code == 0,
                    error = if (code != 0) "mpv exited with code $code." else null,
                    durationMs = _status.value.durationMs,
                )
            }
        }

        if (socket != null) {
            scope.launch { connect(socket) }
        }
    }

    private fun mpvBinary(): String = System.getenv("STARHOLD_MPV")?.takeIf { it.isNotBlank() } ?: "mpv"

    private suspend fun connect(path: String) {
        val address = UnixDomainSocketAddress.of(Path.of(path))
        var ch: SocketChannel? = null
        var attempts = 0
        while (ch == null && attempts < 40 && !released) {
            ch = runCatching { SocketChannel.open(StandardProtocolFamily.UNIX).apply { connect(address) } }.getOrNull()
            if (ch == null) {
                attempts++
                delay(100)
            }
        }
        val opened = ch ?: run {
            _status.value = _status.value.copy(error = "mpv IPC socket did not appear; controls are unavailable.")
            return
        }
        channel = opened
        val buffered = Channels.newReader(opened, StandardCharsets.UTF_8).buffered()
        reader = buffered
        send("""{"command":["observe_property",1,"time-pos"]}""")
        send("""{"command":["observe_property",2,"pause"]}""")
        send("""{"command":["observe_property",3,"duration"]}""")
        send("""{"command":["observe_property",4,"eof-reached"]}""")
        send("""{"command":["set_property","volume",100]}""")
        while (currentCoroutineContext().isActive && !released) {
            val line = runCatching { buffered.readLine() }.getOrNull() ?: break
            if (line.isBlank()) continue
            handleLine(line)
        }
    }

    private fun handleLine(line: String) {
        val obj = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull() ?: return
        when (obj["event"]?.jsonPrimitive?.content) {
            "property-change" -> {
                val name = obj["name"]?.jsonPrimitive?.content ?: return
                val data = obj["data"] ?: return
                when (name) {
                    "time-pos" -> data.jsonPrimitive.doubleOrNull?.let { seconds ->
                        _status.value = _status.value.copy(positionMs = (seconds * 1000).toLong(), buffering = false)
                    }
                    "duration" -> data.jsonPrimitive.doubleOrNull?.let { seconds ->
                        _status.value = _status.value.copy(durationMs = (seconds * 1000).toLong())
                    }
                    "pause" -> data.jsonPrimitive.booleanOrNull?.let { paused ->
                        _status.value = _status.value.copy(playing = !paused)
                    }
                    "eof-reached" -> data.jsonPrimitive.booleanOrNull?.let { ended ->
                        if (ended) _status.value = _status.value.copy(ended = true, playing = false)
                    }
                }
            }
            "end-file" -> {
                val reason = obj["reason"]?.jsonPrimitive?.content
                if (reason == "eof") {
                    _status.value = _status.value.copy(ended = true, playing = false)
                }
            }
            "playback-restart" -> {
                _status.value = _status.value.copy(buffering = false)
            }
        }
    }

    private fun send(payload: String) {
        val ch = channel ?: return
        runCatching {
            synchronized(ch) {
                ch.write(ByteBuffer.wrap((payload + "\n").toByteArray(StandardCharsets.UTF_8)))
            }
        }
    }

    override fun play() {
        send("""{"command":["set_property","pause",false]}""")
        _status.value = _status.value.copy(playing = true, ended = false)
    }

    override fun pause() {
        send("""{"command":["set_property","pause",true]}""")
        _status.value = _status.value.copy(playing = false)
    }

    override fun seekTo(positionMs: Long) {
        send("""{"command":["seek",${positionMs / 1000.0},"absolute"]}""")
        _status.value = _status.value.copy(positionMs = positionMs, ended = false)
    }

    override fun setVolume(volume: Float) {
        send("""{"command":["set_property","volume",${(volume.coerceIn(0f, 1f) * 100)}]}""")
    }

    override fun setSpeed(rate: Float) {
        send("""{"command":["set_property","speed",$rate]}""")
    }

    override fun release() {
        released = true
        teardown()
        scope.cancel()
    }

    private fun teardown() {
        runCatching { send("""{"command":["quit"]}""") }
        runCatching { reader?.close() }
        runCatching { channel?.close() }
        reader = null
        channel = null
        runCatching { process?.destroy() }
        process = null
        socketPath?.let { path -> runCatching { Files.deleteIfExists(Path.of(path)) } }
        socketPath = null
    }
}

@Composable
actual fun rememberPlayerEngine(): PlayerEngine {
    val engine = remember { MpvPlayerEngine() }
    DisposableEffect(engine) { onDispose { engine.release() } }
    return engine
}

@Composable
actual fun PlayerSurface(engine: PlayerEngine, modifier: Modifier) {
    val colors = starholdColors
    Box(modifier.background(Color.Black)) {
        val status by engine.status.collectAsState()
        if (status.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    status.error ?: "",
                    style = starholdType.small,
                    color = colors.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text("Playing in mpv", style = starholdType.h2, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Desktop video opens in a native mpv window so hardware decoding and HDR stay intact. " +
                        "Use the controls below or mpv's own keyboard shortcuts.",
                    style = starholdType.small,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
