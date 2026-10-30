package dev.starhold.app.platform

import androidx.compose.runtime.Composable

/** Android hardware/gesture back; a no-op where the OS provides no back button. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
