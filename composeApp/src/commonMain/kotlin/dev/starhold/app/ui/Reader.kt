package dev.starhold.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.starhold.app.AppState
import dev.starhold.app.Navigator
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.app.platform.PlatformBackHandler
import dev.starhold.app.platform.decodeImage
import dev.starhold.core.model.EpubChapter
import dev.starhold.core.model.Item
import dev.starhold.core.text.HtmlToBlocks
import dev.starhold.core.text.TextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private data class ReaderPalette(val bg: Color, val text: Color, val muted: Color, val panel: Color)

@Composable
private fun palette(theme: String): ReaderPalette = when (theme) {
    "light" -> ReaderPalette(Color(0xFFFAF9F5), Color(0xFF1B1B1B), Color(0xFF66605A), Color(0xFFFFFFFF))
    "sepia" -> ReaderPalette(Color(0xFFF4ECD8), Color(0xFF3B2F1E), Color(0xFF7A6A50), Color(0xFFEFE3C8))
    else -> ReaderPalette(starholdColors.bg, starholdColors.text, starholdColors.muted, starholdColors.surface)
}

@Composable
fun ReadScreen(app: AppState, nav: Navigator, itemId: String) {
    val api = app.api ?: return
    val scope = rememberCoroutineScope()

    var item by remember(itemId) { mutableStateOf<Item?>(null) }
    var loading by remember(itemId) { mutableStateOf(true) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }
    var reload by remember(itemId) { mutableIntStateOf(0) }
    var chapters by remember(itemId) { mutableStateOf<List<EpubChapter>>(emptyList()) }
    var chapterIndex by remember(itemId) { mutableIntStateOf(0) }
    var blocks by remember(itemId) { mutableStateOf<List<TextBlock>>(emptyList()) }
    var pages by remember(itemId) { mutableIntStateOf(0) }
    var page by remember(itemId) { mutableIntStateOf(1) }
    var bitmap by remember(itemId) { mutableStateOf<ImageBitmap?>(null) }
    var pageLoading by remember(itemId) { mutableStateOf(false) }
    var fontSize by remember { mutableIntStateOf(19) }
    var theme by remember { mutableStateOf("dark") }
    var chaptersMenu by remember { mutableStateOf(false) }
    var settingsMenu by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val colors = starholdColors
    val palette = palette(theme)
    val scroll = rememberScrollState()

    LaunchedEffect(api, itemId, reload) {
        loading = true
        error = null
        try {
            val loaded = api.item(itemId)
            item = loaded
            val epub = runCatching { api.epubInfo(itemId) }.getOrNull()
            if (epub != null && epub.chapters.isNotEmpty()) {
                chapters = epub.chapters
                chapterIndex = (app.store.local("reader.last.$itemId")?.toIntOrNull() ?: 0)
                    .coerceIn(0, epub.chapters.lastIndex)
            } else {
                val info = api.pageInfo(itemId)
                pages = info.pages
                page = (app.store.local("reader.page.$itemId")?.toIntOrNull() ?: 1)
                    .coerceIn(1, max(1, info.pages))
            }
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(chapters, chapterIndex) {
        if (chapters.isEmpty()) return@LaunchedEffect
        blocks = emptyList()
        val chapter = chapters.getOrNull(chapterIndex) ?: return@LaunchedEffect
        val html = runCatching { api.epubChapterHtml(itemId, chapter.index) }.getOrNull()
        if (html == null) {
            error = "Could not load this chapter."
            return@LaunchedEffect
        }
        blocks = HtmlToBlocks.parse(html)
        app.store.saveLocal("reader.last.$itemId", chapterIndex.toString())
        scroll.scrollTo(0)
    }

    LaunchedEffect(page, pages) {
        if (pages <= 0) return@LaunchedEffect
        pageLoading = true
        bitmap = null
        val bytes = api.bytes(api.readerPageUrl(itemId, page))
        bitmap = bytes?.let { decodeImage(it) }
        pageLoading = false
        app.store.saveLocal("reader.page.$itemId", page.toString())
        zoom = 1f
        offset = Offset.Zero
    }

    PlatformBackHandler(enabled = true) { nav.pop() }

    Box(Modifier.fillMaxSize().background(palette.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(palette.panel).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(AppIcon.ArrowLeft, onClick = { nav.pop() }, contentDescription = "Back", tint = palette.text)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item?.title ?: "Reader",
                        style = starholdType.small,
                        color = palette.text,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (chapters.isNotEmpty()) {
                        Text(
                            chapters.getOrNull(chapterIndex)?.title ?: "Chapter ${chapterIndex + 1}",
                            style = starholdType.caption,
                            color = palette.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else if (pages > 0) {
                        Text("Page $page of $pages", style = starholdType.caption, color = palette.muted)
                    }
                }
                if (chapters.isNotEmpty()) {
                    Box {
                        IconButton(AppIcon.List, onClick = { chaptersMenu = !chaptersMenu }, contentDescription = "Chapters", tint = palette.text)
                        DropdownMenu(expanded = chaptersMenu, onDismissRequest = { chaptersMenu = false }) {
                            chapters.forEachIndexed { index, chapter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            chapter.title.ifBlank { "Chapter ${index + 1}" },
                                            color = if (index == chapterIndex) colors.accent else palette.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        chaptersMenu = false
                                        chapterIndex = index
                                    },
                                )
                            }
                        }
                    }
                }
                Box {
                    IconButton(AppIcon.Settings, onClick = { settingsMenu = !settingsMenu }, contentDescription = "Reading settings", tint = palette.text)
                    DropdownMenu(expanded = settingsMenu, onDismissRequest = { settingsMenu = false }) {
                        DropdownMenuItem(text = { Text("Theme", color = palette.muted) }, onClick = {})
                        listOf("dark" to "Dark", "light" to "Light", "sepia" to "Sepia").forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (theme == key) colors.accent else palette.text) },
                                onClick = { theme = key; settingsMenu = false },
                            )
                        }
                        DropdownMenuItem(text = { Text("Text size", color = palette.muted) }, onClick = {})
                        listOf(16 to "Small", 19 to "Medium", 23 to "Large", 27 to "Extra large").forEach { (size, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (fontSize == size) colors.accent else palette.text) },
                                onClick = { fontSize = size; settingsMenu = false },
                            )
                        }
                    }
                }
            }

            when {
                loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spinner()
                        Spacer(Modifier.height(10.dp))
                        Text("Opening…", style = starholdType.caption, color = palette.muted)
                    }
                }
                error != null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, style = starholdType.small, color = colors.danger)
                        Spacer(Modifier.height(12.dp))
                        SecondaryButton("Retry", onClick = { reload++ }, icon = AppIcon.Refresh)
                    }
                }
                chapters.isNotEmpty() -> Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 720.dp).padding(vertical = 22.dp)) {
                        SelectionContainer {
                            Column {
                                blocks.forEach { block ->
                                    when {
                                        block.heading in 1..6 -> {
                                            Text(
                                                block.text,
                                                fontSize = (fontSize + (7 - block.heading) * 2).sp,
                                                lineHeight = (fontSize * 1.4f).sp,
                                                color = palette.text,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(Modifier.height(10.dp))
                                        }
                                        else -> {
                                            Text(
                                                block.text,
                                                fontSize = fontSize.sp,
                                                lineHeight = (fontSize * 1.65f).sp,
                                                color = palette.text,
                                            )
                                            Spacer(Modifier.height(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                pages > 0 -> Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures { point ->
                                when {
                                    point.x < size.width * 0.3f -> if (page > 1) page -= 1
                                    point.x > size.width * 0.7f -> if (page < pages) page += 1
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                zoom = (zoom * zoomChange).coerceIn(1f, 4f)
                                offset = if (zoom <= 1.01f) Offset.Zero else offset + pan
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!,
                            contentDescription = "Page $page",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoom,
                                    scaleY = zoom,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                ),
                        )
                    }
                    if (pageLoading) Spinner(size = 28.dp, color = colors.accent)
                }
                else -> EmptyState("Nothing to read", "This item has no readable chapters or pages.")
            }

            if (chapters.isNotEmpty() || pages > 0) {
                Column(Modifier.fillMaxWidth().background(palette.panel).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (pages > 0) {
                        Slider(
                            value = page.toFloat(),
                            onValueChange = { value -> page = value.toInt().coerceIn(1, pages) },
                            valueRange = 1f..max(1, pages).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = palette.muted.copy(alpha = 0.3f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chapters.isNotEmpty()) {
                            SecondaryButton(
                                "Previous",
                                onClick = { if (chapterIndex > 0) chapterIndex -= 1 },
                                enabled = chapterIndex > 0,
                                icon = AppIcon.ChevronLeft,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${chapterIndex + 1} / ${chapters.size}",
                                style = starholdType.caption,
                                color = palette.muted,
                            )
                            Spacer(Modifier.weight(1f))
                            SecondaryButton(
                                "Next chapter",
                                onClick = { if (chapterIndex < chapters.lastIndex) chapterIndex += 1 },
                                enabled = chapterIndex < chapters.lastIndex,
                                icon = AppIcon.ChevronRight,
                            )
                        } else {
                            SecondaryButton("Previous", onClick = { if (page > 1) page -= 1 }, enabled = page > 1, icon = AppIcon.ChevronLeft)
                            Spacer(Modifier.weight(1f))
                            Text("$page / $pages", style = starholdType.caption, color = palette.muted)
                            Spacer(Modifier.weight(1f))
                            SecondaryButton("Next", onClick = { if (page < pages) page += 1 }, enabled = page < pages, icon = AppIcon.ChevronRight)
                        }
                    }
                }
            }
        }
    }
}
