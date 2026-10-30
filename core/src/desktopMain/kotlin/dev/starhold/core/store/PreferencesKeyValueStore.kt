package dev.starhold.core.store

import java.util.prefs.Preferences

/** Durable desktop store backed by the platform Preferences API. */
class PreferencesKeyValueStore(
    node: String = "dev/starhold/native",
) : KeyValueStore {
    private val prefs: Preferences = Preferences.userRoot().node(node)

    override fun getString(key: String): String? = prefs.get(key, null)

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        runCatching { prefs.flush() }
    }

    override fun remove(key: String) {
        prefs.remove(key)
        runCatching { prefs.flush() }
    }
}
