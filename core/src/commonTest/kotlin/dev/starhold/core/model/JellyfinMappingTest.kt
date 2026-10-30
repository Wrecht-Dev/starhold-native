package dev.starhold.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JellyfinMappingTest {
    @Test
    fun episodeMapping() {
        val item = JellyfinItem(
            id = "ep1",
            name = "Pilot",
            type = "Episode",
            productionYear = 2008,
            runTimeTicks = 2_400_000_0000L, // 2400 s
            seriesName = "Breaking Bad",
            parentIndexNumber = 1,
            indexNumber = 1,
            userData = JellyfinUserData(played = false, playbackPositionTicks = 600_000_0000L),
        ).toItem()
        assertEquals("episode", item.type)
        assertEquals(2400, item.runtimeSeconds)
        assertEquals(600, item.positionSeconds)
        assertEquals(1, item.seasonNumber)
        assertEquals(1, item.episodeNumber)
        assertEquals("Breaking Bad", item.series)
        assertTrue(item.isPlayable)
    }

    @Test
    fun movieMappingAndImages() {
        val item = JellyfinItem(
            id = "m1",
            name = "Arrival",
            type = "Movie",
            hasPrimaryImage = true,
            imageTags = kotlinx.serialization.json.JsonObject(emptyMap()),
        ).toItem()
        assertEquals("movie", item.type)
        assertTrue(item.hasImage)
        assertEquals(0, item.runtimeSeconds)
        assertEquals(null, item.positionSeconds)
    }

    @Test
    fun unknownTypeFallsBack() {
        val item = JellyfinItem(id = "x", name = "", type = "Trailer").toItem()
        assertEquals("trailer", item.type)
        assertEquals("Untitled", item.title)
    }
}
