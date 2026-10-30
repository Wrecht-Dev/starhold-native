package dev.starhold.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.starhold.app.design.Tokens
import dev.starhold.app.platform.decodeImage
import dev.starhold.core.network.StarholdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small LRU-ish cache. Posters repeat across rails and detail screens, so a
 * bounded map avoids re-downloading while bounding memory.
 */
object ImageMemoryCache {
    private const val MAX = 128
    private val map = LinkedHashMap<String, ImageBitmap>()
    private val insertion = ArrayDeque<String>()

    fun get(key: String): ImageBitmap? = map[key]

    fun put(key: String, value: ImageBitmap) {
        if (map.put(key, value) == null) insertion.addLast(key)
        while (map.size > MAX && insertion.isNotEmpty()) {
            map.remove(insertion.removeFirst())
        }
    }
}

/**
 * Authenticated remote image. `path` is a server-relative URL (usually from
 * [StarholdApi.imageUrl]). Absent or broken art renders as a quiet surface, not
 * a broken-image icon.
 */
@Composable
fun RemoteImage(
    api: StarholdApi?,
    path: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<ImageBitmap?>(
        initialValue = path?.let { ImageMemoryCache.get(it) },
        key1 = path,
        key2 = api,
    ) {
        if (path == null || api == null) return@produceState
        val cached = ImageMemoryCache.get(path)
        if (cached != null) {
            value = cached
            return@produceState
        }
        val bytes = api.bytes(path)
        if (bytes == null) return@produceState
        val decoded = withContext(Dispatchers.Default) { decodeImage(bytes) }
        if (decoded != null) {
            ImageMemoryCache.put(path, decoded)
            value = decoded
        }
    }

    Box(modifier = modifier.background(Tokens.Surface2)) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = if (contentDescription != null) {
                    Modifier.fillMaxSize().semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.fillMaxSize()
                },
            )
        }
    }
}
