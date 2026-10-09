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
        if (trimmed.isEmpty()) return null

        val cleaned = trimmed.removePrefix("- ").removePrefix("* ").trim()
        val start = cleaned.indexOf("{")
        val end = cleaned.lastIndexOf("}")
        if (start == -1 || end == -1 || start >= end) return null
        val jsonString = cleaned.substring(start, end + 1)

        return try {
            val element = json.parseToJsonElement(jsonString).jsonObject
            val type = element["type"]?.jsonPrimitive?.content

            when (type) {
                "card" -> json.decodeFromString<CardJsonItem>(jsonString)
                "mcq" -> json.decodeFromString<McqJsonItem>(jsonString)
                "point" -> json.decodeFromString<KeyPointJsonItem>(jsonString)
                "short" -> json.decodeFromString<ShortAnswerJsonItem>(jsonString)
                else -> {
                    if (element.containsKey("verdict") && element.containsKey("score")) {
                        json.decodeFromString<GradingResultJson>(jsonString)
                    } else {
                        null
                    }
                }
            }
        } catch (_: Exception) {
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
        val start = trimmed.indexOf("{")
        val end = trimmed.lastIndexOf("}")
        if (start == -1 || end == -1 || start >= end) return null
        val jsonString = trimmed.substring(start, end + 1)
        return try {
            json.decodeFromString<GradingResultJson>(jsonString)
        } catch (_: Exception) {
            null
        }
    }
}
