package dev.starhold.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.platform.PlatformBackHandler
import dev.starhold.app.ui.BrowseScreen
import dev.starhold.app.ui.DetailScreen
import dev.starhold.app.ui.GenresScreen
import dev.starhold.app.ui.HistoryScreen
import dev.starhold.app.ui.HomeScreen
import dev.starhold.app.ui.LiveTvScreen
import dev.starhold.app.ui.ReadScreen
import dev.starhold.app.ui.SearchScreen
import dev.starhold.app.ui.SettingsScreen
import dev.starhold.app.ui.WatchScreen
import dev.starhold.app.ui.Wordmark
import dev.starhold.core.model.Library

private data class Destination(val label: String, val icon: AppIcon, val screen: Screen)

@Composable
private fun libraryIcon(type: String): AppIcon = when (type.lowercase()) {
    "movies", "movie" -> AppIcon.Film
    "tv", "tvshows", "shows", "series" -> AppIcon.Tv
    "music" -> AppIcon.Music
    "books", "book" -> AppIcon.Book
    "comics", "comic" -> AppIcon.Comic
    "games", "game" -> AppIcon.Game
    else -> AppIcon.Grid
}

@Composable
private fun destinations(app: AppState): List<Destination> = buildList {
    add(Destination("Home", AppIcon.Home, Screen.Home))
    add(Destination("Live TV", AppIcon.Cast, Screen.LiveTv))
    app.libraries.forEach { library: Library ->
        add(Destination(library.name, libraryIcon(library.type), Screen.Library(library.id)))
    }
}

@Composable
fun ShellScreen(app: AppState, nav: Navigator) {
    PlatformBackHandler(enabled = nav.canGoBack) { nav.pop() }
    val colors = starholdColors
    val navPosition = app.preferences.navPosition
    val dests = destinations(app)
    val scope = rememberCoroutineScope()

    fun open(screen: Screen) {
        when (screen) {
            is Screen.Home -> nav.reset(Screen.Home)
            is Screen.Library -> nav.reset(screen)
            else -> nav.push(screen)
        }
    }

    // The player is edge-to-edge; every other screen stays clear of system bars.
    val fullscreenPlayer = nav.current is Screen.Watch
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .then(
                if (fullscreenPlayer) Modifier
                else Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            ),
    ) {
        // Phones (including landscape) always get the bottom bar: a top bar is
        // awkward to reach and disappears under the status bar with edge-to-edge.
        val compact = maxWidth < 760.dp || maxHeight < 520.dp
        val effectiveNavPosition = if (compact) "bottom" else navPosition
        val content: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize()) {
                ScreenContent(app, nav)
            }
        }
        when (effectiveNavPosition) {
            "bottom" -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { content() }
                BottomNavigation(app, nav, dests, onOpen = ::open)
            }
            "left", "right" -> Row(Modifier.fillMaxSize()) {
                if (effectiveNavPosition == "left") {
                    SideNavigation(app, nav, dests, compact = compact, onOpen = ::open)
                }
                Box(Modifier.weight(1f)) { content() }
                if (effectiveNavPosition == "right") {
                    SideNavigation(app, nav, dests, compact = compact, onOpen = ::open)
                }
            }
            else -> Column(Modifier.fillMaxSize()) {
                TopNavigation(
                    app = app,
                    nav = nav,
                    destinations = dests,
                    compact = compact,
                    onOpen = ::open,
                    bottom = false,
                )
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}

@Composable
private fun ScreenContent(app: AppState, nav: Navigator) {
    when (val screen = nav.current) {
        Screen.Home -> HomeScreen(app, nav)
        is Screen.Library -> BrowseScreen(app, nav, screen.id)
        is Screen.Detail -> DetailScreen(app, nav, screen.id)
        is Screen.Watch -> WatchScreen(app, nav, screen.id, live = screen.live, liveTitle = screen.liveTitle)
        is Screen.Read -> ReadScreen(app, nav, screen.id)
        is Screen.Search -> SearchScreen(app, nav, screen.query)
        Screen.Genres -> GenresScreen(app, nav)
        Screen.History -> HistoryScreen(app, nav)
        Screen.LiveTv -> LiveTvScreen(app, nav)
        Screen.Settings -> SettingsScreen(app, nav)
        Screen.Landing, Screen.Login, is Screen.Signup -> HomeScreen(app, nav)
    }
}

@Composable
private fun NavPill(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = starholdColors
    Surface(
        modifier = Modifier
            .clip(Tokens.Radius.pill.let { RoundedCornerShape(it) })
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Tokens.Radius.pill),
        color = if (selected) colors.surface2 else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, colors.lineStrong) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                destination.icon,
                size = 17.dp,
                tint = if (selected) colors.accent else colors.muted,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                destination.label,
                style = starholdType.label,
                color = if (selected) colors.text else colors.muted,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TopNavigation(
    app: AppState,
    nav: Navigator,
    destinations: List<Destination>,
    compact: Boolean,
    onOpen: (Screen) -> Unit,
    bottom: Boolean,
) {
    val colors = starholdColors
    val selected = nav.current
    Column(Modifier.fillMaxWidth().background(colors.bg)) {
        if (!bottom) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(modifier = Modifier.clickable { onOpen(Screen.Home) })
                Spacer(Modifier.width(20.dp))
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(destinations) { destination ->
                        NavPill(
                            destination = destination,
                            selected = isSameSection(selected, destination.screen),
                            onClick = { onOpen(destination.screen) },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                TopActions(app, nav, compact)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        } else {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(destinations) { destination ->
                        NavPill(
                            destination = destination,
                            selected = isSameSection(selected, destination.screen),
                            onClick = { onOpen(destination.screen) },
                        )
                    }
                }
                TopActions(app, nav, compact)
            }
        }
    }
}

private fun isSameSection(current: Screen, destination: Screen): Boolean = when {
    current is Screen.Home && destination is Screen.Home -> true
    current is Screen.Library && destination is Screen.Library -> current.id == destination.id
    current is Screen.LiveTv && destination is Screen.LiveTv -> true
    else -> false
}

@Composable
private fun TopActions(app: AppState, nav: Navigator, compact: Boolean) {
    val colors = starholdColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        dev.starhold.app.ui.IconButton(
            AppIcon.Search,
            onClick = { nav.push(Screen.Search("")) },
            contentDescription = "Search",
            tint = colors.muted,
        )
        if (!compact) {
            dev.starhold.app.ui.IconButton(
                AppIcon.Settings,
                onClick = { nav.push(Screen.Settings) },
                contentDescription = "Settings",
                tint = colors.muted,
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surface2)
                .border(1.dp, colors.lineStrong, CircleShape)
                .clickable { nav.push(Screen.Settings) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                app.session?.username?.take(1)?.uppercase() ?: "?",
                style = starholdType.label,
                color = colors.text,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BottomNavigation(
    app: AppState,
    nav: Navigator,
    destinations: List<Destination>,
    onOpen: (Screen) -> Unit,
) {
    val colors = starholdColors
    val selected = nav.current
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(destinations) { destination ->
                    val isSelected = isSameSection(selected, destination.screen)
                    Column(
                        modifier = Modifier
                            .width(74.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.surface2 else Color.Transparent)
                            .clickable { onOpen(destination.screen) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(
                            destination.icon,
                            size = 21.dp,
                            tint = if (isSelected) colors.accent else colors.muted,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            destination.label,
                            style = starholdType.caption,
                            color = if (isSelected) colors.text else colors.muted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                dev.starhold.app.ui.IconButton(
                    AppIcon.Search,
                    onClick = { nav.push(Screen.Search("")) },
                    contentDescription = "Search",
                    tint = colors.muted,
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .border(1.dp, colors.lineStrong, CircleShape)
                        .clickable { nav.push(Screen.Settings) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        app.session?.username?.take(1)?.uppercase() ?: "?",
                        style = starholdType.caption,
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun SideNavigation(
    app: AppState,
    nav: Navigator,
    destinations: List<Destination>,
    compact: Boolean,
    onOpen: (Screen) -> Unit,
) {
    val colors = starholdColors
    val selected = nav.current
    Column(
        modifier = Modifier
            .width(if (compact) 78.dp else 224.dp)
            .fillMaxSize()
            .background(colors.surface)
            .padding(vertical = 16.dp, horizontal = if (compact) 8.dp else 12.dp),
    ) {
        if (!compact) {
            Wordmark(modifier = Modifier.padding(horizontal = 8.dp).clickable { onOpen(Screen.Home) })
            Spacer(Modifier.height(24.dp))
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AppIcon(AppIcon.StarFilled, size = 26.dp, tint = colors.accent)
            }
            Spacer(Modifier.height(22.dp))
        }
        destinations.forEach { destination ->
            val isSelected = isSameSection(selected, destination.screen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.sm))
                    .background(if (isSelected) colors.surface2 else Color.Transparent)
                    .clickable { onOpen(destination.screen) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    destination.icon,
                    size = 19.dp,
                    tint = if (isSelected) colors.accent else colors.muted,
                )
                if (!compact) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        destination.label,
                        style = starholdType.small,
                        color = if (isSelected) colors.text else colors.muted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius.sm))
                .clickable { nav.push(Screen.Settings) }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(AppIcon.Settings, size = 19.dp, tint = colors.muted)
            if (!compact) {
                Spacer(Modifier.width(12.dp))
                Text("Settings", style = starholdType.small, color = colors.muted)
            }
        }
        if (!compact) {
            Spacer(Modifier.height(8.dp))
            Text(
                app.session?.username ?: "",
                style = starholdType.caption,
                color = colors.faint,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}
