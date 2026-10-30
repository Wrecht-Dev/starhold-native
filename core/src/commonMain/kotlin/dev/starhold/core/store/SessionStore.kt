package dev.starhold.core.store

import dev.starhold.core.model.Session
import dev.starhold.core.model.UiPreferences
import kotlinx.serialization.json.Json

/**
 * Persists the active session, the last server URL, and a local mirror of the
 * account's UI preferences so the shell can render before the network answers.
 */
class SessionStore(
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    fun loadSession(): Session? {
        val raw = store.getString(KEY_SESSION) ?: return null
        return runCatching { json.decodeFromString<Session>(raw) }.getOrNull()?.takeIf { it.token.isNotBlank() }
    }

    fun saveSession(session: Session) {
        store.putString(KEY_SESSION, json.encodeToString(Session.serializer(), session))
    }

    fun clearSession() {
        store.remove(KEY_SESSION)
    }

    var serverUrl: String?
        get() = store.getString(KEY_SERVER)?.takeIf { it.isNotBlank() }
        set(value) {
            if (value.isNullOrBlank()) store.remove(KEY_SERVER) else store.putString(KEY_SERVER, value)
        }

    fun loadPreferences(): UiPreferences? {
        val raw = store.getString(KEY_PREFS) ?: return null
        return runCatching { json.decodeFromString<UiPreferences>(raw) }.getOrNull()
    }

    fun savePreferences(prefs: UiPreferences) {
        store.putString(KEY_PREFS, json.encodeToString(UiPreferences.serializer(), prefs))
    }

    fun clearPreferences() {
        store.remove(KEY_PREFS)
    }

    /** Small local values that are device state, not account state. */
    fun local(key: String): String? = store.getString(key)

    fun saveLocal(key: String, value: String) {
        store.putString(key, value)
    }

    /** Volume (0..1) remembered across items and sessions. */
    fun loadVolume(): Float = store.getString(KEY_VOLUME)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f

    fun saveVolume(volume: Float) {
        store.putString(KEY_VOLUME, volume.coerceIn(0f, 1f).toString())
    }

    private companion object {
        const val KEY_SESSION = "starhold.session"
        const val KEY_SERVER = "starhold.server"
        const val KEY_PREFS = "starhold.prefs"
        const val KEY_VOLUME = "starhold.volume"
    }
}

/** Normalises a user-entered server address into an absolute base URL. */
fun normalizeServerUrl(input: String): String? {
    var s = input.trim()
    if (s.isEmpty()) return null
    if (!s.startsWith("http://") && !s.startsWith("https://")) {
        s = "http://$s"
    }
    s = s.trimEnd('/')
    // Reject obviously malformed values early so the UI can explain.
    val afterScheme = s.substringAfter("://", "")
    if (afterScheme.isBlank()) return null
    val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    if (host.isBlank() || host.startsWith(".")) return null
    return s
}
