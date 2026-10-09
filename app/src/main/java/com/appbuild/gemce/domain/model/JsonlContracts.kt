package com.appbuild.gemce.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ItemType {
    @SerialName("card")
    CARD,
    @SerialName("mcq")
    MCQ,
    @SerialName("point")
    POINT,
    @SerialName("short")
    SHORT
}

@Serializable
enum class LanguageMode(val code: String, val label: String) {
    EN("en", "English"),
    TAGLISH("taglish", "Taglish"),
    FIL("fil", "Filipino"),
    CEB("ceb", "Bisaya (Experimental)")
}

@Serializable
data class CardJsonItem(
    val front: String,
    val back: String,
    val evidence: String,
    val type: String = "card"
)

@Serializable
data class McqJsonItem(
    val stem: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
    val evidence: String,
    val type: String = "mcq"
)

@Serializable
data class KeyPointJsonItem(
    val text: String,
    val evidence: String,
    val type: String = "point"
)

@Serializable
data class ShortAnswerJsonItem(
    val stem: String,
    @SerialName("reference_answer")
    val referenceAnswer: String,
    val rubric: List<String> = emptyList(),
    val evidence: String,
    val type: String = "short"
)

@Serializable
data class GradingResultJson(
    val verdict: String, // "correct", "partial", "incorrect"
    val score: Float,     // 0.0 to 1.0
    val feedback: String,
    val missing: List<String> = emptyList()
)
