package dev.starhold.core.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlTest {
    @Test
    fun paragraphsAndHeadings() {
        val blocks = HtmlToBlocks.parse(
            """
            <html><head><style>p{}</style></head><body>
            <h1>Chapter One</h1>
            <p>It was a <i>dark</i> and stormy night.</p>
            <p>Second &amp; last.</p>
            <div>Trailing<br/>line</div>
            </body></html>
            """.trimIndent(),
        )
        assertEquals(TextBlock("Chapter One", 1), blocks[0])
        assertEquals("It was a dark and stormy night.", blocks[1].text)
        assertEquals("Second & last.", blocks[2].text)
        assertEquals("Trailing line", blocks[3].text)
    }

    @Test
    fun scriptsAreDropped() {
        val blocks = HtmlToBlocks.parse("<p>one</p><script>alert('x')</script><p>two</p>")
        assertEquals(listOf("one", "two"), blocks.map { it.text })
    }

    @Test
    fun emptyHtml() {
        assertTrue(HtmlToBlocks.parse("").isEmpty())
        assertTrue(HtmlToBlocks.parse("<p>   </p>").isEmpty())
    }

    @Test
    fun htmlWithoutTagsIsOneBlock() {
        assertEquals(listOf(TextBlock("plain text")), HtmlToBlocks.parse("plain text"))
    }
}
