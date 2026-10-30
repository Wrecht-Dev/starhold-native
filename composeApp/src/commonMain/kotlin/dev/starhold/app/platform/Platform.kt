package dev.starhold.app.platform

import androidx.compose.ui.graphics.ImageBitmap

/** Human-readable host name for settings/diagnostics. */
expect fun platformDisplayName(): String

/** True on Android TV / leanback devices and TV-sized fixed displays. */
expect fun isTelevision(): Boolean

/** Opens a URL in the user's default browser (settings, downloads, errors). */
expect fun openExternally(url: String)

/** Decodes encoded image bytes into a Compose bitmap. */
expect fun decodeImage(bytes: ByteArray): ImageBitmap?
