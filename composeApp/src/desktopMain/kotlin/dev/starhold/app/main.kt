package dev.starhold.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.starhold.core.store.PreferencesKeyValueStore

fun main() = application {
    val store = PreferencesKeyValueStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Starhold",
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        StarholdApp(store)
    }
}
