package dev.starhold.core

import kotlin.test.Test
import kotlin.test.assertTrue

class SmokeTest {
    @Test
    fun platformIsReported() {
        assertTrue(platformName.isNotBlank())
    }
}
