package com.appbuild.gemce.domain.parser

import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.model.GradingResultJson
import com.appbuild.gemce.domain.model.KeyPointJsonItem
import com.appbuild.gemce.domain.model.McqJsonItem
import com.appbuild.gemce.domain.model.ShortAnswerJsonItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JsonlParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parses a single JSON line into a specific domain item based on its "type" field.
     * Skips malformed or unrecognized lines gracefully without throwing.
     */
    fun parseLine(line: String): Any? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }

        return try {
            val element = json.parseToJsonElement(trimmed).jsonObject
            val type = element["type"]?.jsonPrimitive?.content

            when (type) {
                "card" -> json.decodeFromString<CardJsonItem>(trimmed)
                "mcq" -> json.decodeFromString<McqJsonItem>(trimmed)
                "point" -> json.decodeFromString<KeyPointJsonItem>(trimmed)
                "short" -> json.decodeFromString<ShortAnswerJsonItem>(trimmed)
                else -> {
                    // Check if it's a grading result (has "verdict" and "score")
                    if (element.containsKey("verdict") && element.containsKey("score")) {
                        json.decodeFromString<GradingResultJson>(trimmed)
                    } else {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            // Malformed lines are skipped as per design principle 3 & generation pipeline 5.4
            null
        }
    }

    fun parseCard(line: String): CardJsonItem? = parseLine(line) as? CardJsonItem

    fun parseMcq(line: String): McqJsonItem? = parseLine(line) as? McqJsonItem

    fun parseKeyPoint(line: String): KeyPointJsonItem? = parseLine(line) as? KeyPointJsonItem

    fun parseShortAnswer(line: String): ShortAnswerJsonItem? = parseLine(line) as? ShortAnswerJsonItem

    fun parseGradingResult(line: String): GradingResultJson? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        return try {
            json.decodeFromString<GradingResultJson>(trimmed)
        } catch (_: Exception) {
            null
        }
    }
}
