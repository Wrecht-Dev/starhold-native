package dev.starhold.core.store

import android.content.Context

/** Durable Android store backed by SharedPreferences. */
class AndroidKeyValueStore(
    context: Context,
    name: String = "starhold_native",
) : KeyValueStore {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
