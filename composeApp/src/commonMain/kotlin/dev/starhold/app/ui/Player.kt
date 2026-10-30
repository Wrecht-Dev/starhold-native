package dev.starhold.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.starhold.app.AppState
import dev.starhold.app.Navigator
import dev.starhold.app.Screen
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.app.platform.PlatformBackHandler
import dev.starhold.app.player.PlaybackStatus
import dev.starhold.app.player.PlayerSurface
import dev.starhold.app.player.clientCapabilities
import dev.starhold.app.player.drawsInApp
import dev.starhold.app.player.rememberPlayerEngine
import dev.starhold.core.format.formatClock
import dev.starhold.core.model.ClientCapabilities
import dev.starhold.core.model.Item
import dev.starhold.core.model.PlaybackDecision
import dev.starhold.core.model.SubtitleTrack
import dev.starhold.core.network.MediaRequest
import dev.starhold.core.playback.Cue
import dev.starhold.core.playback.WebVtt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private fun authHeaders(app: AppState): Map<String, String> =
    app.session?.token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()

@Composable
fun WatchScreen(
    app: AppState,
    nav: Navigator,
    itemId: String,
    live: Boolean = false,
    liveTitle: String? = null,
) {
    val api = app.api ?: return
    val engine = rememberPlayerEngine()
    val status by engine.status.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = starholdColors

    var item by remember(itemId) { mutableStateOf<Item?>(null) }
    var decision by remember(itemId) { mutableStateOf<PlaybackDecision?>(null) }
    var mode by remember(itemId) { mutableStateOf("") }
    var loading by remember(itemId) { mutableStateOf(true) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }
    var audioIndex by remember(itemId) { mutableIntStateOf(-1) }
    var startMs by remember(itemId) { mutableLongStateOf(0L) }
    var subtitles by remember(itemId) { mutableStateOf<List<SubtitleTrack>>(emptyList()) }
    var subtitle by remember(itemId) { mutableStateOf<SubtitleTrack?>(null) }
    var cues by remember(itemId) { mutableStateOf<List<Cue>>(emptyList()) }
    var subtitleSize by remember { mutableIntStateOf(22) }
    var controlsVisible by remember { mutableStateOf(true) }
    var audioMenu by remember { mutableStateOf(false) }
    var subMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var next by remember(itemId) { mutableStateOf<Item?>(null) }
    var nextCountdown by remember { mutableIntStateOf(-1) }
    var reload by remember { mutableIntStateOf(0) }
    var transcode by remember(itemId) { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var markedPlayed by remember(itemId) { mutableStateOf(false) }
    var progressSyncPaused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    suspend fun savePosition(position: Long) {
        if (live || position <= 3000) return
        runCatching { api.setState(itemId, positionSeconds = (position / 1000).toInt()) }
    }

    LaunchedEffect(api, itemId, audioIndex, reload, transcode, live) {
        loading = true
        error = null
        if (live) {
            mode = "direct"
            startMs = 0L
            engine.prepare(MediaRequest(api.absolute(api.liveStreamUrl(itemId)), authHeaders(app)), 0L)
            engine.play()
            loading = false
            return@LaunchedEffect
        }
        try {
            val loaded = item ?: api.item(itemId).also { item = it }
            val caps: ClientCapabilities = clientCapabilities()
            val position = engine.status.value.positionMs.takeIf { it > 1000 }
                ?: ((loaded.positionSeconds ?: 0) * 1000L)
            startMs = position
            if (transcode) {
                mode = "cpu"
                val url = api.absolute(api.transcodeUrl(loaded.id, (position / 1000).toInt(), audioIndex))
                engine.prepare(MediaRequest(url, authHeaders(app)), position)
                engine.play()
                loading = false
                return@LaunchedEffect
            }
            val chosen = api.playbackDecision(loaded.id, caps, audioIndex)
            decision = chosen
            subtitles = chosen.subtitles
            if (chosen.mode == "direct" || chosen.mode == "remux") {
                mode = chosen.mode
                val path = if (chosen.mode == "direct") {
                    chosen.directUrl.ifBlank { api.streamUrl(loaded.id) }
                } else {
                    api.remuxUrl(loaded.id, (position / 1000).toInt(), audioIndex, caps)
                }
                engine.prepare(MediaRequest(api.absolute(path), authHeaders(app)), position)
                engine.play()
                if (subtitle == null) subtitle = chosen.subtitles.firstOrNull { it.default }
            } else {
                mode = chosen.mode
                error = chosen.reason.ifBlank {
                    "This file's codecs cannot be played on this device. Try a device that supports them, or convert a copy on the server."
                }
            }
            val seriesId = loaded.seriesId
            if (loaded.type == "episode" && seriesId != null) {
                val all = runCatching { api.episodes(seriesId, null) }.getOrDefault(emptyList())
                val index = all.indexOfFirst { it.id == loaded.id }
                next = all.getOrNull(index + 1)
            }
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(subtitle, mode, startMs) {
        val track = subtitle
        if (track == null) {
            cues = emptyList()
            return@LaunchedEffect
        }
        val path = track.url.ifBlank { api.subtitleUrl(itemId, track.index) }
        val body = api.text(api.absolute(path))
        cues = if (body.isNullOrBlank()) {
            emptyList()
        } else {
            val shift = if (mode == "remux" || mode == "cpu") -startMs else 0L
            WebVtt.parse(body, shift)
        }
    }

    LaunchedEffect(engine, itemId, mode) {
        if (mode.isEmpty() || live) return@LaunchedEffect
        val audioOnly = item?.type in setOf("audio", "audiobook")
        while (isActive) {
            delay(if (audioOnly) 8000 else 5000)
            val s = engine.status.value
            if (!progressSyncPaused && s.playing && s.positionMs > 2000) {
                runCatching { api.setState(itemId, positionSeconds = (s.positionMs / 1000).toInt()) }
            }
        }
    }

    // Watched threshold and Up Next.
    LaunchedEffect(status.positionMs, status.durationMs, status.ended) {
        if (live) return@LaunchedEffect
        val duration = status.durationMs
        val threshold = if (duration > 0) duration * 95 / 100 else Long.MAX_VALUE
        if (!markedPlayed && status.positionMs >= threshold) {
            markedPlayed = true
            progressSyncPaused = true
            runCatching { api.setState(itemId, played = true) }
        }
        if (status.ended) {
            if (!markedPlayed) {
                markedPlayed = true
                runCatching { api.setState(itemId, played = true) }
            }
        }
    }

    LaunchedEffect(status.ended, next) {
        val following = next
        if (live || !status.ended || following == null) {
            nextCountdown = -1
            return@LaunchedEffect
        }
        for (i in 8 downTo 0) {
            nextCountdown = i
            delay(1000)
        }
        nextCountdown = -1
        nav.replace(Screen.Watch(following.id))
    }

    LaunchedEffect(controlsVisible, status.playing) {
        if (controlsVisible && status.playing) {
            delay(3600)
            controlsVisible = false
        }
    }

    DisposableEffect(itemId) {
        onDispose {
            val position = engine.status.value.positionMs
            if (!live) scope.launch { savePosition(position) }
            engine.pause()
        }
    }

    PlatformBackHandler(enabled = true) {
        val position = engine.status.value.positionMs
        scope.launch { savePosition(position) }
        nav.pop()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.Spacebar -> { if (engine.status.value.playing) engine.pause() else engine.play(); true }
                    Key.DirectionLeft -> { engine.seekTo(engine.status.value.positionMs - 10_000); true }
                    Key.DirectionRight -> { engine.seekTo(engine.status.value.positionMs + 10_000); true }
                    Key.Escape -> { nav.pop(); true }
                    else -> false
                }
            },
    ) {
        PlayerSurface(engine, Modifier.fillMaxSize())

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { controlsVisible = !controlsVisible }
                },
        )

        val activeCue = remember(cues, status.positionMs) { WebVtt.activeAt(cues, status.positionMs) }
        if (activeCue != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp, start = 40.dp, end = 40.dp),
            ) {
                Text(
                    activeCue.text,
                    style = starholdType.body.copy(
                        fontSize = subtitleSize.sp,
                        lineHeight = (subtitleSize + 6).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spinner(size = 30.dp, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text("Preparing playback…", style = starholdType.small, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        if (error != null) {
            PlaybackError(
                message = error!!,
                canTranscode = decision?.canTranscode == true && !transcode,
                onRetry = { reload++ },
                onTranscode = { transcode = true; reload++ },
                onBack = {
                    scope.launch {
                        savePosition(engine.status.value.positionMs)
                    }
                    nav.pop()
                },
            )
        }

        AnimatedVisibility(visible = controlsVisible && error == null && !loading) {
            Controls(
                item = item,
                status = status,
                app = app,
                live = live,
                liveTitle = liveTitle,
                nav = nav,
                mode = mode,
                audioTracks = decision?.audioTracks ?: emptyList(),
                subtitles = subtitles,
                subtitle = subtitle,
                audioIndex = audioIndex,
                muted = muted,
                audioMenu = audioMenu,
                subMenu = subMenu,
                moreMenu = moreMenu,
                subtitleSize = subtitleSize,
                onToggleControls = { controlsVisible = !controlsVisible },
                onBack = {
                    scope.launch { savePosition(engine.status.value.positionMs) }
                    nav.pop()
                },
                onPlayPause = { if (status.playing) engine.pause() else engine.play() },
                onSeek = { target ->
                    engine.seekTo(target)
                    markedPlayed = false
                    progressSyncPaused = false
                },
                onSkip = { delta -> engine.seekTo((status.positionMs + delta).coerceAtLeast(0)) },
                onMute = {
                    muted = !muted
                    engine.setVolume(if (muted) 0f else app.volume)
                },
                onAudioMenu = { audioMenu = !audioMenu; subMenu = false; moreMenu = false },
                onSubMenu = { subMenu = !subMenu; audioMenu = false; moreMenu = false },
                onMoreMenu = { moreMenu = !moreMenu; audioMenu = false; subMenu = false },
                onPickAudio = { index ->
                    audioMenu = false
                    engine.pause()
                    audioIndex = index
                },
                onPickSubtitle = { track ->
                    subMenu = false
                    subtitle = track
                },
                onSubtitleSize = { subtitleSize = it; moreMenu = false },
                onSpeed = { speed -> engine.setSpeed(speed); moreMenu = false },
            )
        }

        if (status.buffering && !loading && error == null) {
            Box(Modifier.align(Alignment.TopEnd).padding(20.dp)) { Spinner(size = 22.dp, color = Color.White) }
        }

        if (nextCountdown >= 0 && next != null) {
            UpNext(
                episode = next!!,
                api = app.api,
                seconds = nextCountdown,
                onPlayNow = {
                    val following = next ?: return@UpNext
                    nextCountdown = -1
                    nav.replace(Screen.Watch(following.id))
                },
                onDismiss = { nextCountdown = -1; next = null },
            )
        }
    }
}

@Composable
private fun Controls(
    item: Item?,
    status: PlaybackStatus,
    app: AppState,
    live: Boolean,
    liveTitle: String?,
    nav: Navigator,
    mode: String,
    audioTracks: List<dev.starhold.core.model.MediaStream>,
    subtitles: List<SubtitleTrack>,
    subtitle: SubtitleTrack?,
    audioIndex: Int,
    muted: Boolean,
    audioMenu: Boolean,
    subMenu: Boolean,
    moreMenu: Boolean,
    subtitleSize: Int,
    onToggleControls: () -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onMute: () -> Unit,
    onAudioMenu: () -> Unit,
    onSubMenu: () -> Unit,
    onMoreMenu: () -> Unit,
    onPickAudio: (Int) -> Unit,
    onPickSubtitle: (SubtitleTrack?) -> Unit,
    onSubtitleSize: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
) {
    val colors = starholdColors
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.75f),
                        0.25f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.86f),
                    ),
                )
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onToggleControls() },
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    AppIcon.ArrowLeft,
                    onClick = onBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item?.title ?: liveTitle ?: "Live TV",
                        style = starholdType.h3,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MetaLine(
                        listOfNotNull(
                            item?.series,
                            item?.seasonNumber?.let { s -> item.episodeNumber?.let { e -> "S$s E$e" } },
                            item?.credit,
                            if (live) {
                                "Live stream"
                            } else {
                                when (mode) {
                                    "direct" -> "Direct play"
                                    "remux" -> "Remuxed"
                                    "cpu" -> "Server conversion"
                                    else -> null
                                }
                            },
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                StatusBadge(mode)
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                if (live) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.danger.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("LIVE", style = starholdType.caption, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(liveTitle ?: "Live TV", style = starholdType.small, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.weight(1f))
                        IconButton(if (status.playing) AppIcon.Pause else AppIcon.Play, onClick = onPlayPause, contentDescription = "Play or pause", tint = Color.White, size = 44.dp, iconSize = 22.dp)
                        IconButton(if (muted || app.volume == 0f) AppIcon.Mute else AppIcon.Volume, onClick = onMute, contentDescription = "Mute", tint = Color.White, size = 40.dp)
                        IconButton(AppIcon.ArrowLeft, onClick = onBack, contentDescription = "Back", tint = Color.White, size = 40.dp)
                    }
                }
                if (!live) {
                val duration = status.durationMs.coerceAtLeast(1L)
                val fraction = (status.positionMs.toFloat() / duration).coerceIn(0f, 1f)
                var drag by remember { mutableStateOf<Float?>(null) }
                Slider(
                    value = drag ?: fraction,
                    onValueChange = { value -> drag = value },
                    onValueChangeFinished = {
                        drag?.let { value -> onSeek((value * duration).toLong()) }
                        drag = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatClock((status.positionMs / 1000).toInt()),
                        style = starholdType.label,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "-${formatClock(((duration - status.positionMs) / 1000).toInt().coerceAtLeast(0))}",
                        style = starholdType.label,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(AppIcon.SkipBack, onClick = { onSkip(-10_000) }, contentDescription = "Back 10 seconds", tint = Color.White, size = 44.dp, iconSize = 22.dp)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(if (status.playing) AppIcon.Pause else AppIcon.Play, size = 24.dp, tint = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(AppIcon.SkipForward, onClick = { onSkip(10_000) }, contentDescription = "Forward 10 seconds", tint = Color.White, size = 44.dp, iconSize = 22.dp)
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        if (muted || app.volume == 0f) AppIcon.Mute else AppIcon.Volume,
                        onClick = onMute,
                        contentDescription = "Mute",
                        tint = Color.White,
                        size = 40.dp,
                    )
                    Box {
                        IconButton(AppIcon.Music, onClick = onAudioMenu, contentDescription = "Audio track", tint = Color.White, size = 40.dp)
                        DropdownMenu(expanded = audioMenu, onDismissRequest = { onAudioMenu() }) {
                            if (audioTracks.isEmpty()) {
                                DropdownMenuItem(text = { Text("No alternate audio") }, onClick = {})
                            }
                            audioTracks.forEachIndexed { index, track ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            audioLabel(track, index),
                                            color = if (track.index == audioIndex) colors.accent else colors.text,
                                        )
                                    },
                                    onClick = { onPickAudio(track.index) },
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(AppIcon.Subtitles, onClick = onSubMenu, contentDescription = "Subtitles", tint = Color.White, size = 40.dp)
                        DropdownMenu(expanded = subMenu, onDismissRequest = { onSubMenu() }) {
                            DropdownMenuItem(text = { Text("Off") }, onClick = { onPickSubtitle(null) })
                            subtitles.forEach { track ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            subtitleLabel(track),
                                            color = if (track.index == subtitle?.index) colors.accent else colors.text,
                                        )
                                    },
                                    onClick = { onPickSubtitle(track) },
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(AppIcon.Settings, onClick = onMoreMenu, contentDescription = "Playback settings", tint = Color.White, size = 40.dp)
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { onMoreMenu() }) {
                            DropdownMenuItem(text = { Text("Subtitle size", color = colors.faint) }, onClick = {})
                            DropdownMenuItem(text = { Text("Small", color = if (subtitleSize == 18) colors.accent else colors.text) }, onClick = { onSubtitleSize(18) })
                            DropdownMenuItem(text = { Text("Medium", color = if (subtitleSize == 22) colors.accent else colors.text) }, onClick = { onSubtitleSize(22) })
                            DropdownMenuItem(text = { Text("Large", color = if (subtitleSize == 28) colors.accent else colors.text) }, onClick = { onSubtitleSize(28) })
                            DropdownMenuItem(text = { Text("Speed", color = colors.faint) }, onClick = {})
                            listOf(0.75f, 1f, 1.25f, 1.5f).forEach { speed ->
                                DropdownMenuItem(text = { Text("${speed}x") }, onClick = { onSpeed(speed) })
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

private fun audioLabel(track: dev.starhold.core.model.MediaStream, index: Int): String {
    val language = track.language.ifBlank { "Track ${index + 1}" }
    val title = track.title.takeIf { it.isNotBlank() }
    val codec = track.codec.uppercase().takeIf { it.isNotBlank() }
    val channels = when (track.channels) {
        8 -> "7.1"
        6 -> "5.1"
        2 -> "Stereo"
        1 -> "Mono"
        else -> null
    }
    return listOfNotNull(language, title, codec, channels).joinToString(" · ")
}

private fun subtitleLabel(track: SubtitleTrack): String {
    val language = track.language.ifBlank { "Subtitle ${track.index}" }
    val title = track.title.takeIf { it.isNotBlank() }
    val forced = if (track.forced) "forced" else null
    return listOfNotNull(language, title, forced).joinToString(" · ")
}

@Composable
private fun StatusBadge(mode: String) {
    if (mode.isBlank()) return
    val text = when (mode) {
        "direct" -> "DIRECT"
        "remux" -> "REMUX"
        "cpu" -> "CPU"
        else -> mode.uppercase()
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = starholdType.caption, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlaybackError(
    message: String,
    canTranscode: Boolean,
    onRetry: () -> Unit,
    onTranscode: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = starholdColors
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.86f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(Tokens.Radius.lg))
                .background(colors.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(AppIcon.Warning, size = 30.dp, tint = colors.danger)
            Spacer(Modifier.height(12.dp))
            Text("Playback failed", style = starholdType.h2, color = colors.text)
            Spacer(Modifier.height(8.dp))
            Text(message, style = starholdType.small, color = colors.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Back", onClick = onBack, icon = AppIcon.ArrowLeft)
                SecondaryButton("Retry", onClick = onRetry, icon = AppIcon.Refresh)
                if (canTranscode) {
                    PrimaryButton("Convert on server", onClick = onTranscode)
                }
            }
            if (!canTranscode) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Conversion uses the server CPU and is disabled there by default.",
                    style = starholdType.caption,
                    color = colors.faint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun UpNext(
    episode: Item,
    api: dev.starhold.core.network.StarholdApi?,
    seconds: Int,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = starholdColors
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
        Column(
            Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(Tokens.Radius.md))
                .background(colors.surface)
                .border(1.dp, colors.lineStrong, RoundedCornerShape(Tokens.Radius.md))
                .padding(14.dp),
        ) {
            Text("Up next in ${seconds}s", style = starholdType.caption, color = colors.accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(120.dp).height(68.dp).clip(RoundedCornerShape(6.dp)).background(colors.card),
                ) {
                    if (episode.hasImage) {
                        RemoteImage(api, api?.imageUrl(episode.id, 320), Modifier.fillMaxSize(), episode.title)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        style = starholdType.small,
                        color = colors.text,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            episode.seasonNumber?.let { s -> episode.episodeNumber?.let { e -> "S$s E$e" } },
                            dev.starhold.core.format.formatDuration(episode.runtimeSeconds),
                        ).joinToString(" · "),
                        style = starholdType.caption,
                        color = colors.muted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Play now", onClick = onPlayNow, icon = AppIcon.Play)
                SecondaryButton("Cancel", onClick = onDismiss)
            }
        }
    }
}
