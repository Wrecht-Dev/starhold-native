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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.starhold.core.format.relativeTime
import dev.starhold.core.format.typeLabel
import dev.starhold.core.model.HistoryEntry

@Composable
fun HistoryScreen(app: AppState, nav: Navigator) {
    val api = app.api ?: return
    val colors = starholdColors
    var entries by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(api, reload) {
        loading = true
        error = null
        try {
            entries = api.history()
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nav.canGoBack) {
                IconButton(AppIcon.ArrowLeft, onClick = { nav.pop() }, contentDescription = "Back", tint = colors.muted)
                Spacer(Modifier.width(6.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("History", style = starholdType.h1, color = colors.text)
                MetaLine(listOfNotNull(if (entries.isEmpty()) null else "${entries.size} recently watched"))
            }
            IconButton(AppIcon.Refresh, onClick = { reload++ }, contentDescription = "Refresh", tint = colors.muted)
        }
        Spacer(Modifier.height(12.dp))
        when {
            loading -> LoadingBlock("Loading history")
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorNotice(error!!, onRetry = { reload++ })
            }
            entries.isEmpty() -> EmptyState("Nothing watched yet", "Titles you start watching appear here with your progress.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Tokens.Radius.sm))
                            .background(colors.surface)
                            .clickable { nav.push(Screen.Detail(entry.id)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(colors.surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(iconForType(entry.type), size = 18.dp, tint = colors.muted)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.title,
                                style = starholdType.small,
                                color = colors.text,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(
                                    typeLabel(entry.type),
                                    if (entry.played) "Watched" else if (entry.positionSeconds > 0) formatDuration(entry.positionSeconds) else null,
                                    relativeTime(entry.updatedAt).takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                style = starholdType.caption,
                                color = colors.muted,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        AppIcon(AppIcon.ChevronRight, size = 15.dp, tint = colors.faint)
                    }
                }
            }
        }
    }
}
