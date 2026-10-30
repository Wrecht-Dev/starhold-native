package dev.starhold.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import dev.starhold.core.format.formatDuration
import dev.starhold.core.format.plural
import dev.starhold.core.format.typeLabel
import dev.starhold.core.model.EpubChapter
import dev.starhold.core.model.Item
import dev.starhold.core.network.ItemQuery
import dev.starhold.core.network.StarholdApi
import kotlinx.coroutines.launch

private data class DetailPayload(
    val item: Item? = null,
    val seasons: List<Item> = emptyList(),
    val episodes: List<Item> = emptyList(),
    val tracks: List<Item> = emptyList(),
    val similar: List<Item> = emptyList(),
    val chapters: List<EpubChapter> = emptyList(),
    val pages: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
)

@Composable
fun DetailScreen(app: AppState, nav: Navigator, itemId: String) {
    val api = app.api
    var payload by remember(itemId) { mutableStateOf(DetailPayload()) }
    var selectedSeason by remember(itemId) { mutableStateOf<String?>(null) }
    var reload by remember(itemId) { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = starholdColors

    LaunchedEffect(api, itemId, reload) {
        if (api == null) return@LaunchedEffect
        payload = payload.copy(loading = true, error = null)
        try {
            val item = api.item(itemId)
            var seasons = emptyList<Item>()
            var episodes = emptyList<Item>()
            var tracks = emptyList<Item>()
            var chapters = emptyList<EpubChapter>()
            var pages = 0
            when (item.type) {
                "series" -> {
                    seasons = runCatching { api.seasons(item.id) }.getOrDefault(emptyList())
                    val season = seasons.firstOrNull()
                    selectedSeason = season?.id
                    if (season != null) {
                        episodes = runCatching { api.episodes(item.id, season.id) }.getOrDefault(emptyList())
                    }
                }
                "season" -> episodes = runCatching {
                    api.episodes(item.seriesId ?: item.parentId ?: "", item.id)
                }.getOrDefault(emptyList())
                "album" -> tracks = runCatching {
                    api.items(ItemQuery(parentId = item.id, limit = 500, sort = "name"))
                }.getOrDefault(emptyList())
                "book" -> chapters = runCatching { api.epubInfo(item.id).chapters }.getOrDefault(emptyList())
                "comic" -> pages = runCatching { api.pageInfo(item.id).pages }.getOrDefault(0)
            }
            val similar = runCatching { api.similar(item.id) }.getOrDefault(emptyList())
            payload = DetailPayload(
                item = item,
                seasons = seasons,
                episodes = episodes,
                tracks = tracks,
                similar = similar,
                chapters = chapters,
                pages = pages,
                loading = false,
            )
        } catch (e: Throwable) {
            payload = payload.copy(loading = false, error = errorText(e))
        }
    }

    fun toggle(favorite: Boolean? = null, played: Boolean? = null) {
        val item = payload.item ?: return
        if (busy) return
        busy = true
        scope.launch {
            runCatching {
                api?.setState(
                    id = item.id,
                    played = played,
                    favorite = favorite,
                    positionSeconds = if (played == false) 0 else null,
                )
            }.onSuccess {
                payload = payload.copy(
                    item = item.copy(
                        favorite = favorite ?: item.favorite,
                        played = played ?: item.played,
                        positionSeconds = if (played == false) null else item.positionSeconds,
                    ),
                )
            }.onFailure { /* keep old state; the next load reconciles */ }
            busy = false
        }
    }

    fun chooseSeason(season: Item) {
        val item = payload.item ?: return
        selectedSeason = season.id
        scope.launch {
            val episodes = runCatching { api?.episodes(item.id, season.id) ?: emptyList() }
                .getOrDefault(emptyList())
            payload = payload.copy(episodes = episodes)
        }
    }

    when {
        payload.loading -> LoadingBlock()
        payload.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorNotice(payload.error!!, onRetry = { reload++ })
        }
        payload.item != null -> {
            val item = payload.item!!
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 860.dp
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    DetailHeader(item = item, api = api, nav = nav)
                    Column(Modifier.padding(horizontal = if (wide) 36.dp else 18.dp)) {
                        if (wide) {
                            Row {
                                Poster(api = api, item = item)
                                Spacer(Modifier.width(24.dp))
                                Column(Modifier.weight(1f).padding(top = 18.dp)) {
                                    TitleBlock(item)
                                    ActionRow(
                                        item = item,
                                        payload = payload,
                                        app = app,
                                        nav = nav,
                                        busy = busy,
                                        onFavorite = { toggle(favorite = !item.favorite) },
                                        onPlayed = { toggle(played = !item.played) },
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    Overview(item)
                                }
                            }
                        } else {
                            TitleBlock(item)
                            ActionRow(
                                item = item,
                                payload = payload,
                                app = app,
                                nav = nav,
                                busy = busy,
                                onFavorite = { toggle(favorite = !item.favorite) },
                                onPlayed = { toggle(played = !item.played) },
                            )
                            Spacer(Modifier.height(18.dp))
                            Overview(item)
                        }

                        Spacer(Modifier.height(26.dp))
                        when (item.type) {
                            "series" -> {
                                if (payload.seasons.isNotEmpty()) {
                                    SectionTitle("Seasons")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        payload.seasons.forEach { season ->
                                            Chip(
                                                season.title.ifBlank { "Season ${season.seasonNumber ?: "?"}" },
                                                selected = season.id == selectedSeason,
                                                onClick = { chooseSeason(season) },
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                                if (payload.episodes.isNotEmpty()) {
                                    SectionTitle("Episodes")
                                    payload.episodes.forEachIndexed { index, episode ->
                                        EpisodeRow(episode, api) { nav.push(Screen.Watch(episode.id)) }
                                        if (index != payload.episodes.lastIndex) DividerLine()
                                    }
                                } else {
                                    EmptyState("No episodes", "The server did not return episodes for this series.")
                                }
                            }
                            "season" -> {
                                if (payload.episodes.isNotEmpty()) {
                                    SectionTitle("Episodes")
                                    payload.episodes.forEachIndexed { index, episode ->
                                        EpisodeRow(episode, api) { nav.push(Screen.Watch(episode.id)) }
                                        if (index != payload.episodes.lastIndex) DividerLine()
                                    }
                                }
                            }
                            "album" -> {
                                if (payload.tracks.isNotEmpty()) {
                                    SectionTitle(plural(payload.tracks.size, "Track", "Tracks"))
                                    payload.tracks.forEachIndexed { index, track ->
                                        TrackRow(track, index + 1) { nav.push(Screen.Watch(track.id)) }
                                        if (index != payload.tracks.lastIndex) DividerLine()
                                    }
                                }
                            }
                            "book" -> {
                                SectionTitle("Book")
                                Text(
                                    if (payload.chapters.isEmpty()) "Read in the built-in reader." else
                                        "${plural(payload.chapters.size, "chapter", "chapters")} · read in the built-in reader",
                                    style = starholdType.small,
                                    color = colors.muted,
                                )
                                Spacer(Modifier.height(14.dp))
                                PrimaryButton(
                                    "Read",
                                    onClick = { nav.push(Screen.Read(item.id)) },
                                    icon = AppIcon.Book,
                                )
                            }
                            "comic" -> {
                                SectionTitle("Comic")
                                Text(
                                    if (payload.pages > 0) "${plural(payload.pages, "page", "pages")} · page-by-page reader" else
                                        "Open the page reader.",
                                    style = starholdType.small,
                                    color = colors.muted,
                                )
                                Spacer(Modifier.height(14.dp))
                                PrimaryButton(
                                    "Read",
                                    onClick = { nav.push(Screen.Read(item.id)) },
                                    icon = AppIcon.Comic,
                                )
                            }
                        }

                        if (payload.similar.isNotEmpty()) {
                            Spacer(Modifier.height(34.dp))
                            Rail(
                                title = "More like this",
                                items = payload.similar,
                                api = api,
                                nav = nav,
                                showProgress = false,
                            )
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(item: Item, api: StarholdApi?, nav: Navigator) {
    val colors = starholdColors
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        if (item.hasImage) {
            RemoteImage(api, api?.backdropUrl(item.id, 1600), Modifier.fillMaxSize(), item.title)
        } else {
            Box(Modifier.fillMaxSize().background(colors.surface))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.25f),
                    0.6f to colors.bg.copy(alpha = 0.55f),
                    1f to colors.bg,
                ),
            ),
        )
        IconButton(
            AppIcon.ArrowLeft,
            onClick = { nav.pop() },
            contentDescription = "Back",
            tint = Color.White,
            size = 40.dp,
            background = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun Poster(api: StarholdApi?, item: Item) {
    val colors = starholdColors
    Box(
        Modifier
            .padding(top = 18.dp)
            .width(196.dp)
            .height(294.dp)
            .clip(RoundedCornerShape(Tokens.Radius.md))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(Tokens.Radius.md)),
    ) {
        if (item.hasImage) {
            RemoteImage(api, api?.imageUrl(item.id, 400), Modifier.fillMaxSize(), item.title)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppIcon(iconForType(item.type), size = 40.dp, tint = colors.faint)
            }
        }
    }
}

@Composable
private fun TitleBlock(item: Item) {
    val colors = starholdColors
    Text(
        item.title,
        style = starholdType.display,
        color = colors.text,
        fontWeight = FontWeight.ExtraBold,
    )
    if (!item.tagline.isNullOrBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(item.tagline!!, style = starholdType.small, color = colors.accent, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(8.dp))
    MetaLine(
        listOfNotNull(
            typeLabel(item.type),
            item.year?.toString(),
            formatDuration(item.runtimeSeconds),
            item.officialRating,
            item.seasonNumber?.let { s -> item.episodeNumber?.let { e -> "S$s E$e" } },
        ),
    )
    if (item.genres.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item.genres.take(5).forEach { genre -> Badge(genre) }
        }
    }
}

@Composable
private fun Overview(item: Item) {
    val colors = starholdColors
    if (!item.plot.isNullOrBlank()) {
        Text(item.plot!!, style = starholdType.body, color = colors.muted, modifier = Modifier.widthIn(max = 900.dp))
        Spacer(Modifier.height(14.dp))
    }
    Column {
        PersonRow("Credits", item.credit ?: "")
        if (item.series != null) PersonRow("Series", item.series!!)
        if (item.artist != null && item.albumArtist != null) PersonRow("Album artist", item.albumArtist!!)
    }
}

@Composable
private fun ActionRow(
    item: Item,
    payload: DetailPayload,
    app: AppState,
    nav: Navigator,
    busy: Boolean,
    onFavorite: () -> Unit,
    onPlayed: () -> Unit,
) {
    val colors = starholdColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            item.isPlayable -> {
                val resume = (item.positionSeconds ?: 0) > 2
                PrimaryButton(
                    text = if (resume) "Resume" else "Play",
                    icon = AppIcon.Play,
                    large = true,
                    onClick = { nav.push(Screen.Watch(item.id)) },
                )
            }
            item.isReadable -> PrimaryButton(
                "Read",
                icon = AppIcon.Book,
                large = true,
                onClick = { nav.push(Screen.Read(item.id)) },
            )
            item.type == "series" -> {
                val first = payload.episodes.firstOrNull()
                if (first != null) {
                    PrimaryButton(
                        "Play S${first.seasonNumber ?: 1} E${first.episodeNumber ?: 1}",
                        icon = AppIcon.Play,
                        large = true,
                        onClick = { nav.push(Screen.Watch(first.id)) },
                    )
                }
            }
            item.type == "album" -> {
                val first = payload.tracks.firstOrNull()
                if (first != null) {
                    PrimaryButton(
                        "Play album",
                        icon = AppIcon.Play,
                        large = true,
                        onClick = { nav.push(Screen.Watch(first.id)) },
                    )
                }
            }
        }
        IconButton(
            icon = if (item.favorite) AppIcon.StarFilled else AppIcon.Star,
            onClick = onFavorite,
            contentDescription = if (item.favorite) "Remove from favourites" else "Add to favourites",
            tint = if (item.favorite) colors.accent else colors.text,
            background = colors.surface2,
        )
        IconButton(
            icon = if (item.played) AppIcon.CheckCircle else AppIcon.Check,
            onClick = onPlayed,
            contentDescription = if (item.played) "Mark as unwatched" else "Mark as watched",
            tint = if (item.played) colors.success else colors.text,
            background = colors.surface2,
        )
    }
}

@Composable
private fun EpisodeRow(episode: Item, api: StarholdApi?, onClick: () -> Unit) {
    val colors = starholdColors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(150.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(Tokens.Radius.sm))
                .background(colors.card),
        ) {
            if (episode.hasImage) {
                RemoteImage(api, api?.imageUrl(episode.id, 320), Modifier.fillMaxSize(), episode.title)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppIcon(AppIcon.Play, size = 22.dp, tint = colors.faint)
                }
            }
            val fraction = if (episode.runtimeSeconds > 0) {
                ((episode.positionSeconds ?: 0).toFloat() / episode.runtimeSeconds).coerceIn(0f, 1f)
            } else {
                0f
            }
            if (fraction > 0f) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.5f))) {
                    Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(colors.accent))
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    episode.episodeNumber?.let { "E$it" } ?: "",
                    style = starholdType.label,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                )
                if (episode.episodeNumber != null) Spacer(Modifier.width(8.dp))
                Text(
                    episode.title,
                    style = starholdType.body,
                    color = colors.text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!episode.plot.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    episode.plot!!,
                    style = starholdType.caption,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            MetaLine(listOf(formatDuration(episode.runtimeSeconds), if (episode.played) "Watched" else null))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(AppIcon.Play, size = 16.dp, tint = colors.text)
        }
    }
}

@Composable
private fun TrackRow(track: Item, number: Int, onClick: () -> Unit) {
    val colors = starholdColors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            track.trackNumber?.toString() ?: number.toString(),
            style = starholdType.small,
            color = colors.faint,
            modifier = Modifier.width(32.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(track.title, style = starholdType.body, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(track.artist, formatDuration(track.runtimeSeconds).takeIf { it.isNotBlank() }).joinToString(" · "),
                style = starholdType.caption,
                color = colors.muted,
            )
        }
        if (track.played) AppIcon(AppIcon.Check, size = 14.dp, tint = colors.success)
        Spacer(Modifier.width(10.dp))
        Text(formatDuration(track.runtimeSeconds), style = starholdType.caption, color = colors.faint)
    }
}
