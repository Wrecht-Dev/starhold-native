package dev.starhold.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.starhold.app.AppState
import dev.starhold.app.Navigator
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.core.format.typeLabel
import dev.starhold.core.model.GenreCount
import dev.starhold.core.model.Item
import dev.starhold.core.network.ItemQuery
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 60

@Composable
fun BrowseScreen(app: AppState, nav: Navigator, libraryId: String) {
    val api = app.api
    val library = app.libraries.firstOrNull { it.id == libraryId }
    val colors = starholdColors
    val isMusic = library?.type?.lowercase() == "music"
    val isComic = library?.type?.lowercase() in setOf("comics", "comic")

    var sort by remember(libraryId) { mutableStateOf("name") }
    var genre by remember(libraryId) { mutableStateOf<String?>(null) }
    var genres by remember(libraryId) { mutableStateOf<List<GenreCount>>(emptyList()) }
    var total by remember(libraryId) { mutableStateOf(0) }
    var items by remember(libraryId) { mutableStateOf<List<Item>>(emptyList()) }
    var loading by remember(libraryId) { mutableStateOf(true) }
    var loadingMore by remember(libraryId) { mutableStateOf(false) }
    var exhausted by remember(libraryId) { mutableStateOf(false) }
    var error by remember(libraryId) { mutableStateOf<String?>(null) }
    var reload by remember(libraryId) { mutableStateOf(0) }

    val gridState = rememberLazyGridState()

    LaunchedEffect(api, libraryId, sort, genre, reload) {
        if (api == null) return@LaunchedEffect
        loading = true
        error = null
        items = emptyList()
        exhausted = false
        gridState.scrollToItem(0)
        try {
            if (reload == 0 || genres.isEmpty()) {
                genres = runCatching { api.genres(libraryId) }.getOrDefault(emptyList())
            }
            total = runCatching { api.itemsCount(libraryId) }.getOrDefault(0)
            val first = api.items(
                ItemQuery(
                    libraryId = libraryId,
                    limit = PAGE_SIZE,
                    offset = 0,
                    sort = sort,
                    genre = genre,
                ),
            )
            items = first
            exhausted = first.size < PAGE_SIZE
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    // Infinite scroll: fetch the next page when the viewport nears the end.
    LaunchedEffect(api, libraryId, sort, genre, items.size, exhausted, loading) {
        if (api == null || loading || exhausted || loadingMore) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (!loadingMore && !exhausted && lastVisible >= items.size - 12) {
                    loadingMore = true
                    runCatching {
                        api.items(
                            ItemQuery(
                                libraryId = libraryId,
                                limit = PAGE_SIZE,
                                offset = items.size,
                                sort = sort,
                                genre = genre,
                            ),
                        )
                    }.onSuccess { next ->
                        items = items + next
                        exhausted = next.size < PAGE_SIZE
                    }.onFailure { error = errorText(it) }
                    loadingMore = false
                }
            }
    }

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nav.canGoBack) {
                    IconButton(
                        AppIcon.ArrowLeft,
                        onClick = { nav.pop() },
                        contentDescription = "Back",
                        tint = colors.muted,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        library?.name ?: "Library",
                        style = starholdType.h1,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MetaLine(
                        listOf(
                            library?.type?.let { typeLabel(it.removeSuffix("s")) },
                            if (total > 0 && genre == null) "$total titles" else null,
                            genre,
                        ),
                    )
                }
                IconButton(
                    AppIcon.Search,
                    onClick = { nav.push(dev.starhold.app.Screen.Search("")) },
                    contentDescription = "Search",
                    tint = colors.muted,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SortChip("Name", sort == "name") { sort = "name" }
                Spacer(Modifier.width(6.dp))
                SortChip("Added", sort == "added") { sort = "added" }
                Spacer(Modifier.width(6.dp))
                SortChip("Year", sort == "year") { sort = "year" }
                Spacer(Modifier.width(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Chip("All", genre == null, onClick = { genre = null })
                    }
                    items(genres.take(40)) { g ->
                        Chip(g.name, genre == g.name, onClick = { genre = if (genre == g.name) null else g.name }, count = g.count)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Box(Modifier.fillMaxSize()) {
            when {
                loading -> LoadingBlock("Loading ${library?.name ?: "library"}")
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorNotice(error!!, onRetry = { reload++ })
                }
                items.isEmpty() -> EmptyState(
                    "Nothing here yet",
                    if (genre != null) "No titles in $genre." else "This library has no titles yet. Add media and scan in the admin app.",
                )
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(if (isMusic) 168.dp else 142.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            api = api,
                            nav = nav,
                            width = null,
                            modifier = Modifier.fillMaxWidth(),
                            circle = isMusic && item.type == "artist",
                        )
                    }
                    if (loadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Spinner()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(Tokens.Radius.pill))
            .background(if (selected) starholdColors.surface2 else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = starholdType.label,
            color = if (selected) starholdColors.text else starholdColors.muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
