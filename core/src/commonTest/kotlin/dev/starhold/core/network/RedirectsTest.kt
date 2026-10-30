package dev.starhold.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RedirectsTest {
    @Test
    fun resolvesAbsoluteLocations() {
        assertEquals(
            "https://media.example.com/api/v1/health",
            resolveRedirect("http://media.example.com/api/v1/health", "https://media.example.com/api/v1/health"),
        )
    }

    @Test
    fun resolvesProtocolRelativeLocations() {
        assertEquals(
            "http://media.example.com/login",
            resolveRedirect("http://media.example.com/api/v1/auth/login", "//media.example.com/login"),
        )
    }

    @Test
    fun resolvesRootRelativeLocations() {
        assertEquals(
            "https://media.example.com/api/v1/auth/login",
            resolveRedirect("https://media.example.com/api/v1/auth/login", "/api/v1/auth/login"),
        )
    }

    @Test
    fun keepsNonDefaultPort() {
        assertEquals(
            "http://192.168.1.10:18901/api/v1/health",
            resolveRedirect("http://192.168.1.10:18901/api/v1/health", "/api/v1/health"),
        )
    }

    @Test
    fun rejectsBlankLocations() {
        assertNull(resolveRedirect("https://a", " "))
    }
}
