package dev.starhold.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.starhold.app.AppState
import dev.starhold.app.Navigator
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.core.model.GenreCount
import dev.starhold.core.model.Item
import dev.starhold.core.network.ItemQuery

@Composable
fun GenresScreen(app: AppState, nav: Navigator) {
    val api = app.api ?: return
    val colors = starholdColors
    var genres by remember { mutableStateOf<List<GenreCount>>(emptyList()) }
    var selected by remember { mutableStateOf<GenreCount?>(null) }
    var items by remember { mutableStateOf<List<Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingItems by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(api, reload) {
        loading = true
        error = null
        try {
            genres = api.genres()
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selected) {
        val genre = selected ?: return@LaunchedEffect
        loadingItems = true
        items = runCatching { api.items(ItemQuery(genre = genre.name, limit = 150)) }.getOrDefault(emptyList())
        loadingItems = false
    }

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null || nav.canGoBack) {
                IconButton(
                    AppIcon.ArrowLeft,
                    onClick = { if (selected != null) selected = null else nav.pop() },
                    contentDescription = "Back",
                    tint = colors.muted,
                )
                Spacer(Modifier.width(6.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(selected?.name ?: "Genres", style = starholdType.h1, color = colors.text)
                MetaLine(
                    listOfNotNull(
                        if (selected == null) "${genres.size} genres" else "${items.size} titles",
                    ),
                )
            }
            if (selected != null) {
                Text(
                    "All genres",
                    style = starholdType.label,
                    color = colors.muted,
                    modifier = Modifier.clickable { selected = null }.padding(8.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxSize()) {
            when {
                loading -> LoadingBlock("Loading genres")
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorNotice(error!!, onRetry = { reload++ })
                }
                selected == null -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(200.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(genres, key = { it.name }) { genre ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(Tokens.Radius.sm.let { androidx.compose.foundation.shape.RoundedCornerShape(it) })
                                .background(colors.surface)
                                .clickable { selected = genre }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(genre.name, style = starholdType.small, color = colors.text, fontWeight = FontWeight.SemiBold)
                                Text("${genre.count} titles", style = starholdType.caption, color = colors.muted)
                            }
                            AppIcon(AppIcon.ChevronRight, size = 15.dp, tint = colors.faint)
                        }
                    }
                }
                loadingItems -> LoadingBlock("Loading ${selected?.name}")
                items.isEmpty() -> EmptyState("Nothing here", "No titles tagged ${selected?.name}.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(142.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        MediaCard(item = item, api = api, nav = nav, width = null, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
