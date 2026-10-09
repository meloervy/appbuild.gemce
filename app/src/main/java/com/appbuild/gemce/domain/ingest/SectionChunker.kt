package com.appbuild.gemce.domain.ingest

import java.util.UUID

data class Section(
    val id: String = UUID.randomUUID().toString(),
    val documentTitle: String,
    val order: Int,
    val title: String,
    val text: String,
    val sourceRef: String,
    val wordCount: Int = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
)

object SectionChunker {

    private const val TARGET_WORD_COUNT = 500

    /**
     * Splits raw text into cohesive sections based on headings and paragraph boundaries.
     * Keeps provenance through [sourceRef].
     */
    fun chunk(
        documentTitle: String,
        rawText: String,
        sourceRef: String = "Imported Notes"
    ): List<Section> {
        val normalized = rawText.replace("\r\n", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        val paragraphs = normalized.split("\n\n+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) return emptyList()

        val sections = mutableListOf<Section>()
        var currentChunk = StringBuilder()
        var currentWords = 0
        var sectionOrder = 1

        for (paragraph in paragraphs) {
            val pWords = paragraph.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

            if (currentWords > 0 && (currentWords + pWords) > TARGET_WORD_COUNT) {
                val text = currentChunk.toString().trim()
                sections.add(
                    Section(
                        documentTitle = documentTitle,
                        order = sectionOrder++,
                        title = deriveTitle(text, sectionOrder - 1),
                        text = text,
                        sourceRef = sourceRef
                    )
                )
                currentChunk = StringBuilder()
                currentWords = 0
            }

            if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
            currentChunk.append(paragraph)
            currentWords += pWords
        }

        if (currentChunk.isNotEmpty()) {
            val text = currentChunk.toString().trim()
            sections.add(
                Section(
                    documentTitle = documentTitle,
                    order = sectionOrder,
                    title = deriveTitle(text, sectionOrder),
                    text = text,
                    sourceRef = sourceRef
                )
            )
        }

        return sections
    }

    private fun deriveTitle(sectionText: String, fallbackIndex: Int): String {
        val firstLine = sectionText.lines().firstOrNull()?.trim() ?: ""
        return if (firstLine.length in 3..60 && !firstLine.endsWith(".")) {
            firstLine
        } else {
            "Section $fallbackIndex"
        }
    }
}
