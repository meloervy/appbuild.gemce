package com.appbuild.gemce.domain.prompt

import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.AcademicLevel
import com.appbuild.gemce.domain.profile.StudentProfile

object PromptBuilder {

    fun languageInstruction(language: LanguageMode): String = when (language) {
        LanguageMode.EN -> "Write all items in English."
        LanguageMode.FIL -> "Write all items in Tagalog (Filipino), translating concepts clearly while keeping standard scientific names in English."
        LanguageMode.TAGLISH -> "Write items in conversational Taglish (Filipino + English mix), keeping scientific and technical terms in English."
        LanguageMode.CEB -> "Write items in Bisaya (Cebuano), keeping scientific and technical terms in English."
    }

    fun buildSystemInstruction(
        profile: StudentProfile? = null,
        language: LanguageMode = LanguageMode.EN
    ): String {
        val academicContext = profile?.getFullAcademicContext() ?: "College / University"
        val pedagogicalCalibration = when (profile?.academicLevel) {
            AcademicLevel.JUNIOR_HIGH ->
                "- Student Level: Junior High School (Grades 7-10). Use clear, approachable explanations with intuitive analogies and accessible vocabulary."
            AcademicLevel.SENIOR_HIGH ->
                "- Student Level: Senior High School (${profile.seniorHighStrand ?: "General Strand"}). Connect concepts to strand-relevant applications, preparing the student for university standards."
            AcademicLevel.COLLEGE, null ->
                "- Student Level: College / University (${profile?.collegeCourse ?: "Undergraduate Program"}). Maintain complete academic rigor, analytical precision, and domain-appropriate terminology."
        }

        return """
You are Gemce, an expert offline AI study buddy and academic mentor.
$pedagogicalCalibration
TARGET LANGUAGE: ${language.label} (${languageInstruction(language)})

CORE SYSTEM RULES:
1. Ground EVERYTHING strictly in the provided source text. Never fabricate facts.
2. The "evidence" field MUST contain an exact short quote from the text that proves the concept.
3. If introducing essential outside concepts not in the text, label them "[unverified]".
4. Output format: JSON Lines (JSONL). Emit strictly one JSON object per line. No markdown code blocks, no backticks, no preamble.
""".trimIndent()
    }

    fun buildFlashcardPrompt(
        sectionText: String,
        count: Int = 4,
        language: LanguageMode = LanguageMode.EN,
        profile: StudentProfile? = null
    ): String {
        val systemInstruction = buildSystemInstruction(profile, language)
        return """
$systemInstruction

TASK: Generate exactly $count flashcards from the provided source text.

JSONL SCHEMA:
{"type":"card","front":"Question or concept prompt","back":"Direct explanation or definition","evidence":"Verbatim quote from text"}

SOURCE TEXT:
$sectionText
""".trimIndent()
    }

    fun buildMcqPrompt(
        sectionText: String,
        count: Int = 3,
        language: LanguageMode = LanguageMode.EN,
        profile: StudentProfile? = null
    ): String {
        val systemInstruction = buildSystemInstruction(profile, language)
        return """
$systemInstruction

TASK: Generate exactly $count multiple-choice questions from the provided source text.
RULES:
1. Provide 4 plausible choices for each question.
2. "answer" is the 0-based index (0, 1, 2, or 3) of the correct choice.

JSONL SCHEMA:
{"type":"mcq","stem":"Question text","choices":["Choice A","Choice B","Choice C","Choice D"],"answer":1,"explanation":"Why this is correct","evidence":"Verbatim quote from text"}

SOURCE TEXT:
$sectionText
""".trimIndent()
    }

    fun buildShortAnswerPrompt(
        sectionText: String,
        count: Int = 2,
        language: LanguageMode = LanguageMode.EN,
        profile: StudentProfile? = null
    ): String {
        val systemInstruction = buildSystemInstruction(profile, language)
        return """
$systemInstruction

TASK: Generate exactly $count short-answer questions for testing student comprehension.
RULES:
1. Ask questions that require explaining a concept or mechanism in 1-2 sentences.
2. Provide a model "reference_answer" and a list of key concepts in "rubric".

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
        language: LanguageMode = LanguageMode.EN,
        profile: StudentProfile? = null
    ): String {
        val systemInstruction = buildSystemInstruction(profile, language)
        return """
$systemInstruction

TASK: Evaluate the student's answer fairly against the reference answer and rubric.
RULES:
1. Be strict on facts and lenient on wording. Accept synonyms and colloquial phrasing in English, Filipino, Taglish, or Bisaya.
2. "verdict" must be one of: "correct", "partial", "incorrect".
3. "score" must be a float between 0.0 and 1.0.
4. Provide constructive feedback and list any missing points from the rubric.
5. Return ONLY a single raw JSON object on one line.

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
        language: LanguageMode = LanguageMode.EN,
        profile: StudentProfile? = null
    ): String {
        val systemInstruction = buildSystemInstruction(profile, language)
        val formattedSections = sectionsWithLabels.joinToString("\n\n") { (label, text) ->
            "[$label]:\n$text"
        }
        return """
$systemInstruction

TASK: Answer the student's question based strictly on the provided notes sections.
RULES:
1. Cite source sections using markers like [S1], [S2] whenever referencing facts.
2. If the notes do not contain the answer, state clearly: "Your notes do not cover this topic directly."
3. If providing additional outside background, explicitly label it "[unverified]".
4. Keep the answer clear, encouraging, structured, and calibrated to the student's level.

NOTES:
$formattedSections

STUDENT'S QUESTION:
$question
""".trimIndent()
    }
}
