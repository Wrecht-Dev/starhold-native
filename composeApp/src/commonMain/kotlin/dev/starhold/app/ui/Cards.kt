package dev.starhold.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.starhold.app.Navigator
import dev.starhold.app.Screen
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.LocalIsTelevision
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.platform.PlatformBackHandler
import dev.starhold.core.format.isSquareArt
import dev.starhold.core.format.subtitleFor
import dev.starhold.core.model.Item
import dev.starhold.core.model.Library
import dev.starhold.core.network.StarholdApi
import kotlinx.coroutines.launch

/** Opens the most useful destination for an item: watch, read, or detail. */
fun Navigator.openItem(item: Item) {
    when {
        item.isPlayable -> push(Screen.Watch(item.id))
        item.isReadable -> push(Screen.Read(item.id))
        else -> push(Screen.Detail(item.id))
    }
}

@Composable
fun MediaCard(
    item: Item,
    api: StarholdApi?,
    nav: Navigator,
    modifier: Modifier = Modifier,
    width: Dp? = 150.dp,
    showProgress: Boolean = true,
    showWatched: Boolean = true,
    circle: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = starholdColors
    val isTv = LocalIsTelevision.current
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) colors.accent else colors.line,
        label = "cardBorder",
    )
    val square = circle || isSquareArt(item.type)
    val artPath = if (item.hasImage) api?.imageUrl(item.id, 320) else null
    val progress = item.positionSeconds ?: 0
    val fraction = if (item.runtimeSeconds > 0 && progress > 0) {
        (progress.toFloat() / item.runtimeSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable {
                if (onClick != null) onClick() else nav.openItem(item)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (square) 1f else 2f / 3f)
                .clip(if (circle) CircleShape else RoundedCornerShape(Tokens.Radius.sm))
                .background(colors.card)
                .then(
                    if (focused) Modifier.background(colors.accent.copy(alpha = 0.12f)) else Modifier,
                )
                .then(
                    Modifier.border(
                        BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
                        if (circle) CircleShape else RoundedCornerShape(Tokens.Radius.sm),
                    ),
                ),
        ) {
            if (artPath != null) {
                RemoteImage(
                    api = api,
                    path = artPath,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = item.title,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppIcon(
                        iconForType(item.type),
                        size = if (square) 30.dp else 34.dp,
                        tint = colors.faint,
                    )
                }
            }
            if (showWatched && item.played) {
                CornerBadge(AppIcon.Check, colors.success, Modifier.align(Alignment.TopEnd))
            } else if (item.favorite) {
                CornerBadge(AppIcon.StarFilled, colors.accent, Modifier.align(Alignment.TopEnd))
            }
            if (showProgress && fraction > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .background(colors.accent),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            style = starholdType.small,
            color = colors.text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val sub = subtitleFor(
            item.type,
            item.seasonNumber,
            item.episodeNumber,
            item.credit,
            item.year,
            item.runtimeSeconds,
        )
        if (sub != null) {
            Text(
                sub,
                style = starholdType.caption,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CornerBadge(icon: AppIcon, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, size = 14.dp, tint = tint)
    }
}

fun iconForType(type: String): AppIcon = when (type) {
    "movie" -> AppIcon.Film
    "series", "season", "episode" -> AppIcon.Tv
    "album", "artist", "audio" -> AppIcon.Music
    "book", "audiobook" -> AppIcon.Book
    "comic" -> AppIcon.Comic
    "game" -> AppIcon.Game
    "podcast", "podcastepisode" -> AppIcon.Music
    else -> AppIcon.Grid
}

/**
 * Horizontal shelf with header arrows. Uses a LazyRow so a 2,000-title library
 * still paints quickly; the web's scroll-snap is approximated with spacing.
 */
@Composable
fun Rail(
    title: String,
    items: List<Item>,
    api: StarholdApi?,
    nav: Navigator,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
    onSeeAll: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val colors = starholdColors
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isTv = LocalIsTelevision.current

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = starholdType.h3, color = colors.text)
            Spacer(Modifier.weight(1f))
            if (onSeeAll != null) {
                Text(
                    "See all",
                    style = starholdType.label,
                    color = colors.muted,
                    modifier = Modifier.clickable(onClick = onSeeAll).padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            if (!isTv) {
                IconButton(
                    AppIcon.ChevronLeft,
                    onClick = { scope.launch { state.animateScrollBy(-640f) } },
                    contentDescription = "Scroll $title left",
                    tint = if (state.canScrollBackward) colors.muted else colors.faint.copy(alpha = 0.35f),
                    size = 28.dp,
                    iconSize = 18.dp,
                )
                IconButton(
                    AppIcon.ChevronRight,
                    onClick = { scope.launch { state.animateScrollBy(640f) } },
                    contentDescription = "Scroll $title right",
                    tint = if (state.canScrollForward) colors.muted else colors.faint.copy(alpha = 0.35f),
                    size = 28.dp,
                    iconSize = 18.dp,
                )
            }
        }
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                MediaCard(item = item, api = api, nav = nav, showProgress = showProgress)
            }
        }
    }
}

@Composable
fun PersonRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = starholdType.small, color = starholdColors.faint, modifier = Modifier.width(120.dp))
        Text(value, style = starholdType.small, color = starholdColors.text)
    }
}

@Composable
fun ContinueCard(item: Item, api: StarholdApi?, nav: Navigator) {
    val colors = starholdColors
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(300.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { nav.openItem(item) },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Tokens.Radius.sm))
                .background(colors.card)
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) colors.accent else colors.line),
                    RoundedCornerShape(Tokens.Radius.sm),
                ),
        ) {
            if (item.hasImage) {
                RemoteImage(api, api?.imageUrl(item.id, 480), Modifier.fillMaxSize(), item.title)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppIcon(iconForType(item.type), size = 30.dp, tint = colors.faint)
                }
            }
            val fraction = if (item.runtimeSeconds > 0) {
                ((item.positionSeconds ?: 0).toFloat() / item.runtimeSeconds).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                Box(Modifier.fillMaxWidth(fraction).height(6.dp).background(colors.accent))
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(AppIcon.Play, size = 20.dp, tint = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            style = starholdType.small,
            color = colors.text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.series ?: dev.starhold.core.format.typeLabel(item.type),
            style = starholdType.caption,
            color = colors.muted,
            maxLines = 1,
        )
    }
}
