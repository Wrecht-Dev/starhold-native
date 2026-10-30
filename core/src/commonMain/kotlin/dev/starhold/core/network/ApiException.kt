package dev.starhold.core.network

/** A server error with the HTTP status and the server's machine-readable code. */
class ApiException(
    message: String,
    val status: Int = 0,
    val code: String = "",
) : Exception(message) {
    val isUnauthorized: Boolean get() = status == 401
    val isNotFound: Boolean get() = status == 404
    val isOffline: Boolean get() = status == 0
}

fun offlineException(cause: Throwable? = null): ApiException =
    ApiException(
        message = cause?.message?.takeIf { it.isNotBlank() } ?: "Cannot reach the server",
        status = 0,
    )

/** The server answered 401 and the session must be discarded. */
class SessionExpiredException : Exception("Session expired")
