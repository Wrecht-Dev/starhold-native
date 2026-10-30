package dev.starhold.core.store

import dev.starhold.core.model.Session
import dev.starhold.core.model.UiPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStoreTest {
    @Test
    fun sessionRoundTrips() {
        val store = SessionStore(MemoryKeyValueStore())
        store.saveSession(Session("tok", "ada", "admin", canDownload = false))
        val loaded = store.loadSession()
        assertEquals("tok", loaded?.token)
        assertEquals("ada", loaded?.username)
        assertTrue(loaded!!.isAdmin)
        assertEquals(false, loaded.canDownload)
    }

    @Test
    fun corruptSessionIsDiscarded() {
        val kv = MemoryKeyValueStore()
        kv.putString("starhold.session", "{not json")
        assertNull(SessionStore(kv).loadSession())
    }

    @Test
    fun blankTokenIsNotASession() {
        val kv = MemoryKeyValueStore()
        kv.putString("starhold.session", """{"token":"","username":"ada"}""")
        assertNull(SessionStore(kv).loadSession())
    }

    @Test
    fun preferencesRoundTrip() {
        val store = SessionStore(MemoryKeyValueStore())
        store.savePreferences(UiPreferences(navPosition = "left", density = "compact", accentColor = "#ff8800"))
        val prefs = store.loadPreferences()
        assertEquals("left", prefs?.navPosition)
        assertEquals("#ff8800", prefs?.accentColor)
    }

    @Test
    fun volumeIsClamped() {
        val store = SessionStore(MemoryKeyValueStore())
        store.saveVolume(2f)
        assertEquals(1f, store.loadVolume())
        store.saveVolume(-1f)
        assertEquals(0f, store.loadVolume())
    }

    @Test
    fun serverUrlNormalisation() {
        assertEquals("http://media.lan:8096", normalizeServerUrl("media.lan:8096"))
        assertEquals("https://media.lan", normalizeServerUrl("  https://media.lan/  "))
        assertEquals("http://192.168.1.4:8080", normalizeServerUrl("http://192.168.1.4:8080/"))
        assertNull(normalizeServerUrl("   "))
        assertNull(normalizeServerUrl("http://"))
    }
}
