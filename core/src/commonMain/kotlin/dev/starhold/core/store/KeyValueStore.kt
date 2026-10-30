package dev.starhold.core.store

/**
 * Tiny synchronous key/value abstraction so each platform can use its own
 * durable store without dragging Compose or Android context into `core`.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/** In-memory store for tests and previews. */
class MemoryKeyValueStore(
    private val map: MutableMap<String, String> = mutableMapOf(),
) : KeyValueStore {
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}
