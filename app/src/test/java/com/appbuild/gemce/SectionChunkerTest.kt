package com.appbuild.gemce

import com.appbuild.gemce.domain.ingest.SectionChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionChunkerTest {

    @Test
    fun testEmptyTextReturnsEmptyList() {
        val sections = SectionChunker.chunk("Doc", "")
        assertTrue(sections.isEmpty())
    }

    @Test
    fun testSingleShortParagraphReturnsOneSection() {
        val raw = "Photosynthesis is the process by which green plants and certain other organisms transform light energy into chemical energy."
        val sections = SectionChunker.chunk("Biology", raw, "Page 1")

        assertEquals(1, sections.size)
        val s = sections.first()
        assertEquals("Biology", s.documentTitle)
        assertEquals("Page 1", s.sourceRef)
        assertEquals(1, s.order)
        assertTrue(s.wordCount > 5)
    }

    @Test
    fun testSplittingByParagraphThreshold() {
        val p1 = "A ".repeat(300)
        val p2 = "B ".repeat(300)
        val raw = "$p1\n\n$p2"

        val sections = SectionChunker.chunk("Test Doc", raw)
        assertEquals(2, sections.size)
        assertEquals(1, sections[0].order)
        assertEquals(2, sections[1].order)
    }
}
