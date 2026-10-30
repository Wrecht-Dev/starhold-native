package dev.starhold.core.text

/** One readable chunk of an EPUB chapter, optionally a heading. */
data class TextBlock(val text: String, val heading: Int = 0)

/**
 * Converts EPUB chapter XHTML into plain blocks. The native reader deliberately
 * does not embed a web engine: text, headings, paragraphs and line breaks cover
 * narrative books without shipping a browser.
 */
object HtmlToBlocks {
    private val comment = Regex("(?s)<!--.*?-->")
    private val ignored = Regex("(?is)<(script|style|head)[^>]*>.*?</\\1\\s*>")
    private val tag = Regex("<[^>]+>")
    private val blockTags = setOf(
        "p", "div", "li", "blockquote", "tr", "td", "th", "section", "article",
        "h1", "h2", "h3", "h4", "h5", "h6", "pre", "figure", "figcaption", "ul", "ol", "table",
    )
    private val headingTag = Regex("h[1-6]")

    fun parse(html: String): List<TextBlock> {
        val cleaned = html.replace(comment, "").replace(ignored, "")
        val blocks = mutableListOf<TextBlock>()
        val buffer = StringBuilder()
        var heading = 0
        var cursor = 0

        fun flush() {
            val text = decodeEntities(buffer.toString())
                .replace('\u00a0', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isNotEmpty()) blocks += TextBlock(text, heading)
            buffer.clear()
            heading = 0
        }

        for (match in tag.findAll(cleaned)) {
            buffer.append(cleaned, cursor, match.range.first)
            val raw = match.value
            val closing = raw.startsWith("</")
            val name = raw.removePrefix("<").removePrefix("/")
                .substringBefore('>').substringBefore(' ').substringBefore('\t')
                .removeSuffix("/")
                .lowercase()
            when {
                name == "br" || name == "hr" -> buffer.append(' ')
                !closing && headingTag.matches(name) -> {
                    flush()
                    heading = name.drop(1).toIntOrNull() ?: 0
                }
                closing && name in blockTags -> flush()
                !closing && (name == "p" || name == "div" || name == "blockquote" || name == "li") && buffer.isNotBlank() -> flush()
            }
            cursor = match.range.last + 1
        }
        buffer.append(cleaned, cursor, cleaned.length)
        flush()
        return blocks
    }

    fun plainText(html: String): String = parse(html).joinToString("\n\n") { it.text }

    private fun decodeEntities(value: String): String {
        var text = value
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
        text = Regex("&#(x?)([0-9a-fA-F]+);").replace(text) { match ->
            val code = if (match.groupValues[1].isEmpty()) {
                match.groupValues[2].toIntOrNull()
            } else {
                match.groupValues[2].toIntOrNull(16)
            }
            code?.takeIf { it in 1..0x10FFFF }?.let(::codePointToText) ?: match.value
        }
        return text.replace("&amp;", "&")
    }

    /** Platform-neutral replacement for JVM-only `Character.toChars`. */
    private fun codePointToText(code: Int): String {
        if (code <= 0xFFFF) return code.toChar().toString()
        val offset = code - 0x10000
        val high = (0xD800 + (offset shr 10)).toChar()
        val low = (0xDC00 + (offset and 0x3FF)).toChar()
        return high.toString() + low
    }
}
