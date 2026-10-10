package com.appbuild.gemce.domain.export

import com.appbuild.gemce.domain.model.CardJsonItem

object AnkiExporter {

    /**
     * Exports a list of flashcards into Anki-compatible TSV format.
     * Includes standard Anki headers `#separator:tab` and `#html:true` for seamless one-tap import.
     */
    fun exportToAnkiTsv(cards: List<CardJsonItem>, deckTag: String = "Gemce"): String {
        val sb = StringBuilder()
        sb.append("#separator:tab\n")
        sb.append("#html:true\n")
        sb.append("#tags column:3\n")

        for (card in cards) {
            val front = sanitizeField(card.front)
            val back = sanitizeField(card.back)
            val tag = sanitizeTag(if (card.evidence.isNotBlank()) "${deckTag}::verified" else deckTag)
            sb.append("$front\t$back\t$tag\n")
        }

        return sb.toString()
    }

    /**
     * Exports a list of flashcards into standard CSV format with double-quoted fields.
     */
    fun exportToCsv(cards: List<CardJsonItem>, deckTag: String = "Gemce"): String {
        val sb = StringBuilder()
        sb.append("Front,Back,Evidence/Tag\n")

        for (card in cards) {
            val front = escapeCsvField(card.front)
            val back = escapeCsvField(card.back)
            val tag = escapeCsvField(card.evidence.ifBlank { deckTag })
            sb.append("$front,$back,$tag\n")
        }

        return sb.toString()
    }

    private fun sanitizeField(text: String): String {
        return text
            .replace("\t", " ")
            .replace("\r\n", "<br>")
            .replace("\n", "<br>")
            .trim()
    }

    private fun sanitizeTag(tag: String): String {
        return tag
            .replace(" ", "_")
            .replace("\t", "_")
            .replace("\n", "")
            .trim()
    }

    private fun escapeCsvField(text: String): String {
        val escaped = text.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
