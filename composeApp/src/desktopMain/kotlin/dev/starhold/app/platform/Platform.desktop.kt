package dev.starhold.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Desktop
import java.net.URI

actual fun platformDisplayName(): String = System.getProperty("os.name", "Desktop") + " " +
    System.getProperty("os.version", "")

actual fun isTelevision(): Boolean = false

actual fun openExternally(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
