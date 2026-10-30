package dev.starhold.app.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Set once from the Activity before composing; platform helpers only need app context. */
object AndroidPlatform {
    lateinit var appContext: Context
}

actual fun platformDisplayName(): String {
    val version = android.os.Build.VERSION.RELEASE
    return "Android $version"
}

actual fun isTelevision(): Boolean {
    val ctx = runCatching { AndroidPlatform.appContext }.getOrNull() ?: return false
    val pm = ctx.packageManager
    val leanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val uiMode = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    return leanback || uiMode
}

actual fun openExternally(url: String) {
    runCatching {
        val ctx = AndroidPlatform.appContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
