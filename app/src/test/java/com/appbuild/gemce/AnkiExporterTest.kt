package com.appbuild.gemce

import com.appbuild.gemce.domain.export.AnkiExporter
import com.appbuild.gemce.domain.model.CardJsonItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnkiExporterTest {

    @Test
    fun testExportToAnkiTsvHeaderAndFormatting() {
        val cards = listOf(
            CardJsonItem(
                front = "What is ATP?",
                back = "Universal energy currency",
                evidence = "Mitochondria produce ATP through cellular respiration."
            ),
            CardJsonItem(
                front = "What is Mitosis?",
                back = "Process of cell division\nresulting in two identical daughter cells",
                evidence = ""
            )
        )

        val tsv = AnkiExporter.exportToAnkiTsv(cards, deckTag = "Biology")

        assertTrue(tsv.startsWith("#separator:tab\n#html:true\n#tags column:3\n"))
        assertTrue(tsv.contains("What is ATP?\tUniversal energy currency\tBiology::verified\n"))
        // Check newline converted to <br>
        assertTrue(tsv.contains("Process of cell division<br>resulting in two identical daughter cells\tBiology\n"))
    }

    @Test
    fun testExportToCsvEscaping() {
        val cards = listOf(
            CardJsonItem(
                front = "Quotes test: \"Hello World\"",
                back = "Back with, commas",
                evidence = "Verified fact"
            )
        )

        val csv = AnkiExporter.exportToCsv(cards)
        assertTrue(csv.startsWith("Front,Back,Evidence/Tag\n"))
        assertTrue(csv.contains("\"Quotes test: \"\"Hello World\"\"\",\"Back with, commas\",\"Verified fact\"\n"))
    }
}
