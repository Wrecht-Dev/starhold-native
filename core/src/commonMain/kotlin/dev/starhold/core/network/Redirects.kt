package dev.starhold.core.network

import io.ktor.http.URLBuilder

/** HTTP statuses we resolve manually so POST bodies (login) survive. */
val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)

/**
 * Turns a `Location` header into an absolute URL. Relative locations are
 * resolved against [base], which is the request URL that produced the redirect.
 */
fun resolveRedirect(base: String, location: String): String? {
    val loc = location.trim()
    if (loc.isEmpty()) return null
    if (loc.startsWith("http://") || loc.startsWith("https://")) return loc
    val origin = originOf(base) ?: return null
    val scheme = base.substringBefore("://")
    val path = base.substringAfter("://", "").substringAfter('/', "")
    val directory = if (path.contains('/')) "/" + path.substringBeforeLast('/') else ""
    return when {
        loc.startsWith("//") -> "$scheme:$loc"
        loc.startsWith("/") -> origin + loc
        else -> origin + directory + "/" + loc
    }
}

/** `scheme://host[:port]`, or null when the URL cannot be parsed. */
fun originOf(url: String): String? = runCatching {
    val parsed = URLBuilder(url)
    val port = parsed.port
    val defaultPort = parsed.protocol.defaultPort
    buildString {
        append(parsed.protocol.name)
        append("://")
        append(parsed.host)
        if (port != 0 && port != defaultPort) {
            append(':')
            append(port)
        }
    }
}.getOrNull()
