package dev.starhold.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.starhold.app.AppState
import dev.starhold.app.Navigator
import dev.starhold.app.Screen
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.core.model.LiveChannel
import dev.starhold.core.model.LiveProgramme
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun LiveTvScreen(app: AppState, nav: Navigator) {
    val api = app.api ?: return
    val colors = starholdColors
    val scope = rememberCoroutineScope()

    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var programmes by remember { mutableStateOf<List<LiveProgramme>>(emptyList()) }
    var groups by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var nowEpoch by remember { mutableStateOf(Clock.System.now().epochSeconds) }

    LaunchedEffect(api, reload) {
        loading = true
        error = null
        try {
            val now = Clock.System.now()
            channels = api.liveChannels()
            groups = runCatching { api.liveGroups() }.getOrDefault(emptyList())
            programmes = runCatching {
                api.liveGuide(
                    from = Instant.fromEpochSeconds(now.epochSeconds - 3600).toString(),
                    to = Instant.fromEpochSeconds(now.epochSeconds + 4 * 3600).toString(),
                )
            }.getOrDefault(emptyList())
            nowEpoch = now.epochSeconds
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    // Refresh "now"/progress every 30 s without refetching.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            nowEpoch = Clock.System.now().epochSeconds
        }
    }

    val visible = channels
        .filter { selectedGroup == null || it.group == selectedGroup }
        .sortedWith(compareBy({ it.number }, { it.name }))

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Live TV", style = starholdType.h1, color = colors.text)
                    MetaLine(
                        listOfNotNull(
                            if (channels.isEmpty()) null else "${channels.size} channels",
                            if (programmes.isEmpty()) "no guide data" else "${programmes.size} programmes",
                        ),
                    )
                }
                IconButton(AppIcon.Refresh, onClick = { reload++ }, contentDescription = "Refresh guide", tint = colors.muted)
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Chip("All", selectedGroup == null, onClick = { selectedGroup = null }) }
                items(groups) { group ->
                    Chip(group, selectedGroup == group, onClick = { selectedGroup = if (selectedGroup == group) null else group })
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            loading -> LoadingBlock("Loading channels")
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorNotice(error!!, onRetry = { reload++ })
            }
            channels.isEmpty() -> EmptyState(
                "Live TV is not set up",
                "Add an M3U playlist and optionally an XMLTV guide in the Starhold admin app. Channels and the guide will appear here.",
                action = { SecondaryButton("Retry", { reload++ }, icon = AppIcon.Refresh) },
            )
            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp, end = 24.dp, top = 2.dp, bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id }) { channel ->
                    ChannelRow(
                        api = api,
                        channel = channel,
                        programmes = programmes.filter { it.channelId == channel.id }.sortedBy { it.startAt },
                        nowEpoch = nowEpoch,
                        onPlay = { nav.push(Screen.Watch(channel.id, live = true, liveTitle = channel.name)) },
                        onFavourite = {
                            scope.launch {
                                val next = !channel.favorite
                                runCatching { api.setLiveFavorite(channel.id, next) }
                                    .onSuccess {
                                        channels = channels.map {
                                            if (it.id == channel.id) it.copy(favorite = next) else it
                                        }
                                    }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    api: dev.starhold.core.network.StarholdApi?,
    channel: LiveChannel,
    programmes: List<LiveProgramme>,
    nowEpoch: Long,
    onPlay: () -> Unit,
    onFavourite: () -> Unit,
) {
    val colors = starholdColors
    val now = programmes.firstOrNull { p ->
        val start = parseEpoch(p.startAt)
        val stop = parseEpoch(p.stopAt)
        start != null && stop != null && nowEpoch in start..stop
    }
    val currentEnd = now?.let { parseEpoch(it.stopAt) } ?: nowEpoch
    val next = programmes.firstOrNull { p ->
        val start = parseEpoch(p.startAt)
        start != null && start >= currentEnd
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .background(colors.surface)
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            channel.number.toString(),
            style = starholdType.label,
            color = colors.faint,
            modifier = Modifier.width(34.dp),
        )
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (!channel.logo.isNullOrBlank()) {
                RemoteImage(
                    api = api,
                    path = api?.absolute(channel.logo!!),
                    modifier = Modifier.size(40.dp),
                    contentDescription = channel.name,
                )
            } else {
                Text(
                    channel.name.take(2).uppercase(),
                    style = starholdType.caption,
                    color = colors.muted,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                channel.name,
                style = starholdType.small,
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(listOfNotNull(channel.group, channel.health.takeIf { it != "unknown" }))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.width(300.dp)) {
            if (now != null) {
                val start = parseEpoch(now.startAt) ?: 0
                val stop = parseEpoch(now.stopAt) ?: 1
                val fraction = ((nowEpoch - start).toFloat() / (stop - start).toFloat()).coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        now.title,
                        style = starholdType.small,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatTime(now.startAt),
                        style = starholdType.caption,
                        color = colors.faint,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(colors.surface2),
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(colors.accent))
                }
            } else {
                Text("No guide data", style = starholdType.caption, color = colors.faint)
            }
            if (next != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatTime(next.startAt)}  ${next.title}",
                    style = starholdType.caption,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        IconButton(
            icon = if (channel.favorite) AppIcon.StarFilled else AppIcon.Star,
            onClick = onFavourite,
            contentDescription = if (channel.favorite) "Remove favourite" else "Add favourite",
            tint = if (channel.favorite) colors.accent else colors.faint,
            size = 34.dp,
            iconSize = 16.dp,
        )
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(AppIcon.Play, size = 15.dp, tint = colors.accent)
        }
    }
}

private fun parseEpoch(iso: String): Long? = runCatching { Instant.parse(iso).epochSeconds }.getOrNull()

private fun formatTime(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
