package dev.starhold.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.starhold.app.design.LocalIsTelevision
import dev.starhold.app.design.StarholdTheme
import dev.starhold.app.design.starholdColors
import dev.starhold.app.platform.isTelevision
import dev.starhold.app.ui.AuthFlow
import dev.starhold.app.ui.ServerSetupScreen
import dev.starhold.app.ui.SplashScreen
import dev.starhold.core.store.KeyValueStore
import dev.starhold.core.store.SessionStore

/**
 * Shared application root. Platform entry points provide the durable key/value
 * store; everything else is identical on Android, iOS and desktop.
 */
@Composable
fun StarholdApp(store: KeyValueStore) {
    val scope = rememberCoroutineScope()
    val sessionStore = remember { SessionStore(store) }
    val app = remember { AppState(sessionStore, scope) }
    val navigator = remember { Navigator(Screen.Home) }
    val television = remember { isTelevision() }

    LaunchedEffect(Unit) { app.bootstrap() }

    StarholdTheme(preferences = app.preferences) {
        CompositionLocalProvider(LocalIsTelevision provides television) {
            Surface(Modifier.fillMaxSize(), color = starholdColors.bg) {
                when {
                    app.booting -> SplashScreen()
                    app.serverUrl == null -> ServerSetupScreen(app)
                    app.session == null -> AuthFlow(app) { navigator.reset(Screen.Home) }
                    else -> ShellScreen(app, navigator)
                }
            }
        }
    }
}
