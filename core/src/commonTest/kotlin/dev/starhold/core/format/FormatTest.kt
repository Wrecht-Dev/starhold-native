package dev.starhold.core.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatTest {
    @Test
    fun durations() {
        assertEquals("", formatDuration(0))
        assertEquals("38s", formatDuration(38))
        assertEquals("45m", formatDuration(45 * 60))
        assertEquals("1h 23m", formatDuration(83 * 60))
        assertEquals("2h", formatDuration(120 * 60))
    }

    @Test
    fun clocks() {
        assertEquals("0:00", formatClock(0))
        assertEquals("9:05", formatClock(545))
        assertEquals("1:02:03", formatClock(3723))
        assertEquals("0:00", formatClock(-10))
    }

    @Test
    fun captions() {
        assertEquals("S2 E4", subtitleFor("episode", 2, 4, null, null, 2400))
        assertEquals("Nirvana · 1991", subtitleFor("album", null, null, "Nirvana", 1991, 0))
        assertEquals("1999", subtitleFor("movie", null, null, null, 1999, 0))
        assertNull(subtitleFor("movie", null, null, null, null, 0))
    }

    @Test
    fun pluralisation() {
        assertEquals("1 season", plural(1, "season", "seasons"))
        assertEquals("3 seasons", plural(3, "season", "seasons"))
    }

    @Test
    fun typeLabels() {
        assertEquals("Movie", typeLabel("movie"))
        assertEquals("Track", typeLabel("audio"))
        assertEquals("Podcast", typeLabel("podcast"))
        assertEquals("Mdash", typeLabel("mdash"))
    }
}
