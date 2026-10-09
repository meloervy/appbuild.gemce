package com.appbuild.gemce.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testHeadings() {
        val md = """
            # Heading 1
            ## Heading 2
            ### Heading 3
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals(1, (blocks[0] as MarkdownBlock.Heading).level)
        assertEquals("Heading 1", (blocks[0] as MarkdownBlock.Heading).text)

        assertTrue(blocks[1] is MarkdownBlock.Heading)
        assertEquals(2, (blocks[1] as MarkdownBlock.Heading).level)
        assertEquals("Heading 2", (blocks[1] as MarkdownBlock.Heading).text)

        assertTrue(blocks[2] is MarkdownBlock.Heading)
        assertEquals(3, (blocks[2] as MarkdownBlock.Heading).level)
        assertEquals("Heading 3", (blocks[2] as MarkdownBlock.Heading).text)
    }

    @Test
    fun testFencedCodeBlock() {
        val md = """
            ```kotlin
            val x = 10
            println(x)
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
        val codeBlock = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val x = 10\nprintln(x)", codeBlock.code)
    }

    @Test
    fun testBulletList() {
        val md = """
            - Item 1
            - Item 2 with **bold**
            * Item 3
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.BulletList)
        val list = blocks[0] as MarkdownBlock.BulletList
        assertEquals(3, list.items.size)
        assertEquals("Item 1", list.items[0])
        assertEquals("Item 2 with **bold**", list.items[1])
        assertEquals("Item 3", list.items[2])
    }

    @Test
    fun testNumberedList() {
        val md = """
            1. First step
            2. Second step
            3. Third step
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.NumberedList)
        val list = blocks[0] as MarkdownBlock.NumberedList
        assertEquals(3, list.items.size)
        assertEquals(1, list.items[0].first)
        assertEquals("First step", list.items[0].second)
        assertEquals(2, list.items[1].first)
        assertEquals("Second step", list.items[1].second)
    }

    @Test
    fun testBlockquote() {
        val md = """
            > This is a quote.
            > It spans multiple lines.
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Quote)
        assertEquals("This is a quote.\nIt spans multiple lines.", (blocks[0] as MarkdownBlock.Quote).text)
    }

    @Test
    fun testTable() {
        val md = """
            | Architecture | Pros | Cons |
            | --- | --- | --- |
            | Monolith | Simple deploy | Scaling limits |
            | Microservices | Independent scale | Network complexity |
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Table)
        val table = blocks[0] as MarkdownBlock.Table
        assertEquals(listOf("Architecture", "Pros", "Cons"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Monolith", "Simple deploy", "Scaling limits"), table.rows[0])
        assertEquals(listOf("Microservices", "Independent scale", "Network complexity"), table.rows[1])
    }

    @Test
    fun testDivider() {
        val md = """
            Section 1
            ---
            Section 2
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.Divider)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
    }

    @Test
    fun testComplexMixedLlmMarkdown() {
        val md = """
            ### System Design Overview

            Here is how **horizontal scaling** works compared to **vertical scaling**:

            - **Horizontal**: Add more nodes behind a `load balancer`.
            - **Vertical**: Upgrade CPU and RAM on existing hardware.

            ```java
            public class CacheService {
                private Map<String, Object> cache = new ConcurrentHashMap<>();
            }
            ```

            > Remember: Premature optimization is the root of all evil. [S1]
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(5, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)
        assertTrue(blocks[2] is MarkdownBlock.BulletList)
        assertTrue(blocks[3] is MarkdownBlock.CodeBlock)
        assertTrue(blocks[4] is MarkdownBlock.Quote)

        val quote = blocks[4] as MarkdownBlock.Quote
        assertTrue(quote.text.contains("[S1]"))
    }

    @Test
    fun testInlineFormatting() {
        val annotated = MarkdownParser.buildInlineAnnotatedString("This is **bold**, *italic*, and `val x = 1` with [S1] citation.")
        // Verifies builder executes without exceptions and produces formatted string
        assertTrue(annotated.text.contains("bold"))
        assertTrue(annotated.text.contains("italic"))
        assertTrue(annotated.text.contains("val x = 1"))
        assertTrue(annotated.text.contains("[S1]"))
    }
}
