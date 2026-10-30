package dev.starhold.app.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop uses on-screen back affordances and Esc handling in the shell.
}
