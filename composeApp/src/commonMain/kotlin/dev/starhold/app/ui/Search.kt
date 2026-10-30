package dev.starhold.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import dev.starhold.app.AppState
import dev.starhold.app.Navigator
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.core.model.Item
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(app: AppState, nav: Navigator, initialQuery: String) {
    val api = app.api ?: return
    val colors = starholdColors
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Item>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    LaunchedEffect(query) {
        val term = query.trim()
        if (term.length < 2) {
            results = emptyList()
            searched = false
            loading = false
            return@LaunchedEffect
        }
        delay(280)
        loading = true
        error = null
        try {
            results = api.search(term)
            searched = true
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nav.canGoBack) {
                IconButton(AppIcon.ArrowLeft, onClick = { nav.pop() }, contentDescription = "Back", tint = colors.muted)
                Spacer(Modifier.width(6.dp))
            }
            StarholdTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search",
                placeholder = "Titles, people, genres",
                modifier = Modifier.weight(1f).focusRequester(focus),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            MetaLine(
                listOfNotNull(
                    when {
                        loading -> "Searching…"
                        error != null -> null
                        searched -> "${results.size} results"
                        query.trim().length in 1..2 -> "Type at least two characters"
                        else -> null
                    },
                    query.trim().takeIf { searched && it.isNotBlank() },
                ),
            )
        }
        Box(Modifier.fillMaxSize()) {
            when {
                loading -> LoadingBlock("Searching")
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorNotice(error!!)
                }
                results.isEmpty() && searched -> EmptyState("No matches", "Nothing on this server matches \"${query.trim()}\".")
                results.isEmpty() -> EmptyState(
                    "Search your library",
                    "Find movies, series, episodes, albums, books and comics. Results update as you type.",
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(142.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(results, key = { it.id }) { item ->
                        MediaCard(item = item, api = api, nav = nav, width = null, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
