package dev.starhold.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.starhold.core.model.Library
import dev.starhold.core.model.Session
import dev.starhold.core.model.UiPreferences
import dev.starhold.core.network.ApiException
import dev.starhold.core.network.REDIRECT_STATUSES
import dev.starhold.core.network.StarholdApi
import dev.starhold.core.store.SessionStore
import dev.starhold.core.store.normalizeServerUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Process-wide client state: where the server is, who is signed in, which
 * libraries are visible, and the account's UI preferences. Screens read this
 * and call back into it; it owns no UI.
 */
class AppState(
    val store: SessionStore,
    private val scope: CoroutineScope,
) {
    var serverUrl by mutableStateOf(store.serverUrl)
        private set

    var session by mutableStateOf(store.loadSession())
        private set

    var preferences by mutableStateOf(store.loadPreferences() ?: UiPreferences())
        private set

    var libraries by mutableStateOf<List<Library>>(emptyList())
        private set

    var booting by mutableStateOf(true)
        private set

    var busy by mutableStateOf(false)
        private set

    var api by mutableStateOf<StarholdApi?>(null)
        private set

    /** 0..1, remembered locally across sessions. */
    var volume by mutableStateOf(store.loadVolume())
        private set

    var serverHealth by mutableStateOf<StarholdApi.ServerHealth?>(null)
        private set

    init {
        rebuildApi()
    }

    private fun rebuildApi() {
        api = serverUrl?.let { url -> StarholdApi(url, tokenProvider = { session?.token }) }
    }

    private fun adoptCanonicalUrl(client: StarholdApi) {
        val canonical = client.baseUrl
        if (canonical != serverUrl) {
            serverUrl = canonical
            store.serverUrl = canonical
            rebuildApi()
        }
    }

    suspend fun bootstrap() {
        if (api != null && session != null) {
            runCatching { refreshSession() }.onFailure { error ->
                if (error is ApiException && error.status == 401) signOut()
            }
        }
        booting = false
    }

    /** Validates and remembers a server address. */
    suspend fun connect(raw: String): Result<StarholdApi.ServerHealth> = runCatching {
        val url = normalizeServerUrl(raw)
            ?: throw IllegalArgumentException("Enter a server address such as https://media.example.com")
        val probe = StarholdApi(url, tokenProvider = { null })
        val health = probe.health()
        // The server may have redirected http -> https (or to a canonical host).
        val canonical = probe.baseUrl
        serverUrl = canonical
        store.serverUrl = canonical
        serverHealth = health
        rebuildApi()
        health
    }

    suspend fun login(username: String, password: String, mfaCode: String? = null): Result<Session> =
        runCatching {
            val client = api ?: throw IllegalStateException("No server configured")
            busy = true
            try {
                val created = client.login(username, password, mfaCode)
                session = created
                store.saveSession(created)
                adoptCanonicalUrl(client)
                runCatching { refreshSession() }
                created
            } finally {
                busy = false
            }
        }

    suspend fun refreshSession() {
        val client = api ?: return
        val current = session ?: return
        val me = client.me()
        val updated = current.copy(
            username = me.username.ifBlank { current.username },
            role = me.role.ifBlank { current.role },
            canDownload = me.canDownload ?: current.canDownload,
        )
        session = updated
        store.saveSession(updated)
        libraries = runCatching { client.libraries() }.getOrDefault(libraries)
        runCatching { client.uiPreferences() }.onSuccess { prefs ->
            preferences = prefs
            store.savePreferences(prefs)
        }
    }

    suspend fun refreshLibraries() {
        val client = api ?: return
        libraries = runCatching { client.libraries() }.getOrDefault(libraries)
    }

    fun signOut() {
        val client = api
        session = null
        store.clearSession()
        libraries = emptyList()
        client?.let { c -> scope.launch { runCatching { c.logout() } } }
    }

    /** Called by screens when a request returns 401. */
    fun sessionExpired() {
        signOut()
    }

    fun updateVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        store.saveVolume(volume)
    }

    suspend fun updatePreferences(transform: (UiPreferences) -> UiPreferences) {
        val next = transform(preferences)
        preferences = next
        store.savePreferences(next)
        api?.let { client ->
            runCatching { client.saveUiPreferences(next) }.onSuccess { saved ->
                preferences = saved
                store.savePreferences(saved)
            }
        }
    }
}

/** Maps exceptions to short, non-technical UI copy. */
fun errorText(error: Throwable): String = when (error) {
    is ApiException -> when {
        error.status == 401 -> "Your session expired. Sign in again."
        error.status == 0 -> "Cannot reach the server. Check the address and your network."
        error.status == 403 -> error.message.orEmpty().ifBlank { "You do not have access to that." }
        error.status == 404 -> error.message.orEmpty().ifBlank { "That item no longer exists." }
        error.status in REDIRECT_STATUSES -> {
            val target = error.location?.takeIf { it.isNotBlank() }
            if (target != null) {
                "The server redirects to $target. Update the server address to that URL."
            } else {
                "The server redirected the request (HTTP ${error.status}). Use the final https:// address from your browser."
            }
        }
        else -> error.message.orEmpty().ifBlank { "The server returned an error (${error.status})." }
    }
    else -> error.message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
}
