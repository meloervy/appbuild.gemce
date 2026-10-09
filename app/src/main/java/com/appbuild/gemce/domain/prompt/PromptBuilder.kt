package com.appbuild.gemce.domain.prompt

import com.appbuild.gemce.domain.model.LanguageMode

object PromptBuilder {

    private fun languageInstruction(language: LanguageMode): String = when (language) {
        LanguageMode.EN -> "Write all items in English."
        LanguageMode.TAGLISH -> "Write items in Taglish (conversational Filipino + English), keeping technical and scientific terms in English."
        LanguageMode.FIL -> "Write all items in Filipino (Tagalog), translating concepts clearly."
        LanguageMode.CEB -> "Write items in Bisaya (Cebuano), keeping technical terms in English."
    }

    fun buildFlashcardPrompt(sectionText: String, count: Int = 4, language: LanguageMode = LanguageMode.TAGLISH): String {
        return """
You are Gemce, an AI study buddy. Generate exactly $count flashcards from the provided source text.
${languageInstruction(language)}

RULES:
1. Ground EVERYTHING in the source text.
2. The "evidence" field MUST contain an exact short quote from the text that proves the answer.
3. Emit one JSON object per line. Do NOT output markdown code blocks, backticks, or prose.

JSONL SCHEMA:
{"type":"card","front":"Question or concept prompt","back":"Direct explanation or definition","evidence":"Verbatim quote from text"}

SOURCE TEXT:
$sectionText
""".trimIndent()
    }

    fun buildMcqPrompt(sectionText: String, count: Int = 3, language: LanguageMode = LanguageMode.TAGLISH): String {
        return """
You are Gemce, an AI study buddy. Generate exactly $count multiple-choice questions from the provided source text.
${languageInstruction(language)}

RULES:
1. Provide 4 realistic choices for each question.
2. "answer" is the 0-based index (0, 1, 2, or 3) of the correct choice.
3. "evidence" must be a short quote copied from the source text.
4. Emit one JSON object per line. No markdown code blocks, backticks, or prose.

JSONL SCHEMA:
{"type":"mcq","stem":"Question text","choices":["Choice A","Choice B","Choice C","Choice D"],"answer":1,"explanation":"Why this is correct","evidence":"Verbatim quote from text"}

SOURCE TEXT:
$sectionText
""".trimIndent()
    }

    fun buildShortAnswerPrompt(sectionText: String, count: Int = 2, language: LanguageMode = LanguageMode.TAGLISH): String {
        return """
You are Gemce, an AI study buddy. Generate exactly $count short-answer questions for testing student comprehension.
${languageInstruction(language)}

RULES:
1. Ask questions that require explaining a concept or mechanism in 1-2 sentences.
2. Provide a model "reference_answer" and a list of key concepts in "rubric".
3. "evidence" must be a short quote copied from the source text.
4. Emit one JSON object per line. No markdown code blocks, backticks, or prose.

JSONL SCHEMA:
{"type":"short","stem":"Question prompt","reference_answer":"Ideal complete answer","rubric":["key concept 1","key concept 2"],"evidence":"Verbatim quote from text"}

SOURCE TEXT:
$sectionText
""".trimIndent()
    }

    fun buildGradingPrompt(
        questionStem: String,
        referenceAnswer: String,
        rubric: List<String>,
        studentAnswer: String,
        language: LanguageMode = LanguageMode.TAGLISH
    ): String {
        return """
You are Gemce, an AI grader. Evaluate the student's answer fairly against the reference answer and rubric.
${languageInstruction(language)}

RULES:
1. Be strict on facts and lenient on wording. Accept synonyms and colloquial phrasing in English, Filipino, Taglish, or Bisaya.
2. "verdict" must be one of: "correct", "partial", "incorrect".
3. "score" must be a float between 0.0 and 1.0.
4. Provide constructive feedback and list any missing points from the rubric.
5. Return ONLY a single raw JSON object on one line. No markdown blocks.

JSON SCHEMA:
{"verdict":"correct","score":1.0,"feedback":"Helpful explanation of the grade","missing":[]}

QUESTION:
$questionStem

REFERENCE ANSWER:
$referenceAnswer

RUBRIC POINTS:
${rubric.joinToString("\n- ", prefix = "- ")}

STUDENT'S ANSWER:
$studentAnswer
""".trimIndent()
    }

    fun buildAskGemcePrompt(
        sectionsWithLabels: List<Pair<String, String>>, // Pair("S1", "section text...")
        question: String,
        language: LanguageMode = LanguageMode.TAGLISH
    ): String {
        val formattedSections = sectionsWithLabels.joinToString("\n\n") { (label, text) ->
            "[$label]:\n$text"
        }
        return """
You are Gemce, an offline AI study buddy. Answer the student's question based strictly on the provided notes sections.
${languageInstruction(language)}

RULES:
1. Cite source sections using markers like [S1], [S2] whenever referencing facts.
2. If the notes do not contain the answer, state clearly: "Your notes do not cover this topic directly."
3. If providing additional outside knowledge, explicitly label it "[unverified]".
4. Keep the answer clear, encouraging, and structured.

NOTES:
$formattedSections

STUDENT'S QUESTION:
$question
""".trimIndent()
    }
}
