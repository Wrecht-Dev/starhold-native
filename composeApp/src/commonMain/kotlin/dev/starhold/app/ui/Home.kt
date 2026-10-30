package dev.starhold.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import dev.starhold.core.model.Item
import dev.starhold.core.model.Library
import dev.starhold.core.network.ItemQuery
import dev.starhold.core.network.StarholdApi
import kotlinx.coroutines.delay

private data class HomePayload(
    val hero: List<Item> = emptyList(),
    val continueWatching: List<Item> = emptyList(),
    val latest: List<Item> = emptyList(),
    val rails: List<Pair<Library, List<Item>>> = emptyList(),
)

@Composable
fun HomeScreen(app: AppState, nav: Navigator) {
    val api = app.api
    var payload by remember { mutableStateOf(HomePayload()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var heroIndex by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(api, app.libraries, reload) {
        if (api == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val resume = runCatching { api.resume() }.getOrDefault(emptyList())
            val latest = runCatching { api.latest() }.getOrDefault(emptyList())
            val rails = app.libraries.map { library ->
                library to runCatching {
                    api.items(ItemQuery(libraryId = library.id, limit = 14, sort = "added"))
                }.getOrDefault(emptyList())
            }
            val heroPool = (latest + rails.flatMap { it.second })
                .filter { it.hasImage && !it.plot.isNullOrBlank() && (it.type == "movie" || it.type == "series") }
                .distinctBy { it.id }
                .shuffled()
                .take(12)
            payload = HomePayload(
                hero = heroPool,
                continueWatching = resume.filter { it.hasImage && it.type != "episode" },
                latest = latest.filter { it.hasImage }.take(14),
                rails = rails.filter { it.second.isNotEmpty() },
            )
            heroIndex = 0
        } catch (e: Throwable) {
            error = errorText(e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(payload.hero, paused) {
        if (payload.hero.size < 2 || paused) return@LaunchedEffect
        while (true) {
            delay(Tokens.HERO_ROTATE_MS)
            heroIndex = (heroIndex + 1) % payload.hero.size
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 760.dp
        val heroHeight: Dp = (maxHeight * 0.64f).coerceIn(440.dp, 780.dp)
        val horizontal = if (compact) 16.dp else 32.dp

        when {
            loading -> LoadingBlock("Loading your library")
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorNotice(error!!, onRetry = { reload++ })
                }
            }
            app.libraries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    "Your library is empty",
                    "Add libraries in the Starhold admin app, run a scan, then come back. Nothing is shipped with the server by default.",
                    action = { SecondaryButton("Retry", { reload++ }, icon = AppIcon.Refresh) },
                )
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                val hero = payload.hero.getOrNull(heroIndex)
                if (hero != null) {
                    Hero(
                        item = hero,
                        api = api,
                        nav = nav,
                        height = heroHeight,
                        compact = compact,
                        paused = paused,
                        index = heroIndex,
                        count = payload.hero.size,
                        onTogglePause = { paused = !paused },
                        onPick = { heroIndex = it; paused = false },
                    )
                } else {
                    Spacer(Modifier.height(24.dp))
                    PageTitle("Home", "Everything on this server")
                }

                Column(
                    modifier = Modifier.padding(horizontal = horizontal),
                    verticalArrangement = Arrangement.spacedBy(30.dp),
                ) {
                    Spacer(Modifier.height(6.dp))
                    if (payload.continueWatching.isNotEmpty()) {
                        Rail(
                            title = "Continue watching",
                            items = payload.continueWatching,
                            api = api,
                            nav = nav,
                            onSeeAll = { nav.push(Screen.History) },
                        )
                    }
                    if (payload.latest.isNotEmpty()) {
                        Rail(title = "Recently added", items = payload.latest, api = api, nav = nav)
                    }
                    payload.rails.forEach { (library, items) ->
                        Rail(
                            title = library.name,
                            items = items,
                            api = api,
                            nav = nav,
                            onSeeAll = { nav.push(Screen.Library(library.id)) },
                        )
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun Hero(
    item: Item,
    api: StarholdApi?,
    nav: Navigator,
    height: Dp,
    compact: Boolean,
    paused: Boolean,
    index: Int,
    count: Int,
    onTogglePause: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val colors = starholdColors
    Box(Modifier.fillMaxWidth().height(height)) {
        if (item.hasImage) {
            RemoteImage(
                api = api,
                path = api?.backdropUrl(item.id, 1600),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(colors.surface))
        }

        // Dual scrim: dark from the left for copy, dark at the bottom into the page.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to colors.bg.copy(alpha = 0.97f),
                    0.45f to colors.bg.copy(alpha = 0.80f),
                    0.75f to colors.bg.copy(alpha = 0.25f),
                    1f to Color.Transparent,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to colors.bg.copy(alpha = 0.25f),
                    1f to colors.bg.copy(alpha = 0.98f),
                ),
            ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = if (compact) 18.dp else 40.dp,
                    end = if (compact) 18.dp else 40.dp,
                    bottom = if (compact) 30.dp else 46.dp,
                )
                .widthIn(max = 720.dp),
        ) {
            Text(
                item.title,
                style = if (compact) starholdType.h1 else starholdType.display,
                color = colors.text,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            MetaLine(
                listOfNotNull(
                    item.year?.toString(),
                    formatDuration(item.runtimeSeconds),
                    item.officialRating,
                    item.genres.take(3).joinToString(", ").takeIf { it.isNotBlank() },
                ),
            )
            if (!item.plot.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    item.plot!!,
                    style = starholdType.small,
                    color = colors.muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val resume = (item.positionSeconds ?: 0) > 2
                PrimaryButton(
                    text = when {
                        resume -> "Resume"
                        item.type == "series" -> "View series"
                        else -> "Play"
                    },
                    icon = if (item.type == "series") null else AppIcon.Play,
                    onClick = {
                        if (item.type == "series") nav.push(Screen.Detail(item.id)) else nav.push(Screen.Watch(item.id))
                    },
                    large = true,
                )
                SecondaryButton(
                    "Details",
                    onClick = { nav.push(Screen.Detail(item.id)) },
                    icon = AppIcon.Info,
                )
            }
        }

        if (count > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = if (compact) 16.dp else 36.dp, bottom = if (compact) 34.dp else 50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    icon = if (paused) AppIcon.Play else AppIcon.Pause,
                    onClick = onTogglePause,
                    contentDescription = if (paused) "Resume hero rotation" else "Pause hero rotation",
                    tint = colors.muted,
                    size = 30.dp,
                    iconSize = 15.dp,
                )
                for (i in 0 until count) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .clickable { onPick(i) }
                            .padding(vertical = 6.dp, horizontal = 1.dp),
                    ) {
                        Box(
                            Modifier
                                .size(width = if (i == index) 20.dp else 7.dp, height = 7.dp)
                                .clip(CircleShape)
                                .background(if (i == index) colors.accent else colors.lineStrong),
                        )
                    }
                }
            }
        }
    }
}
