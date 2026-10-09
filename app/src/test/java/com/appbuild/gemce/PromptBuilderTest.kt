package com.appbuild.gemce

import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.AcademicLevel
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.prompt.PromptBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun testFlashcardPromptContainsEvidenceRuleAndProfile() {
        val profile = StudentProfile(
            academicLevel = AcademicLevel.SENIOR_HIGH,
            seniorHighStrand = "STEM",
            preferredLanguage = LanguageMode.TAGLISH
        )
        val prompt = PromptBuilder.buildFlashcardPrompt("Sample source text", 5, LanguageMode.TAGLISH, profile)
        assertTrue(prompt.contains("evidence"))
        assertTrue(prompt.contains("JSONL"))
        assertTrue(prompt.contains("Taglish"))
        assertTrue(prompt.contains("Senior High School"))
        assertTrue(prompt.contains("STEM"))
    }

    @Test
    fun testMcqPromptContainsChoicesAndAnswer() {
        val prompt = PromptBuilder.buildMcqPrompt("Sample text", 3, LanguageMode.EN)
        assertTrue(prompt.contains("choices"))
        assertTrue(prompt.contains("answer"))
        assertTrue(prompt.contains("English"))
    }

    @Test
    fun testShortAnswerPromptContainsReferenceAnswerAndRubric() {
        val prompt = PromptBuilder.buildShortAnswerPrompt("Sample text", 2, LanguageMode.FIL)
        assertTrue(prompt.contains("reference_answer"))
        assertTrue(prompt.contains("rubric"))
        assertTrue(prompt.contains("Filipino"))
    }

    @Test
    fun testGradingPromptStructure() {
        val prompt = PromptBuilder.buildGradingPrompt(
            questionStem = "Define ATP",
            referenceAnswer = "Adenosine triphosphate",
            rubric = listOf("energy currency", "phosphate groups"),
            studentAnswer = "It is the cell's energy currency."
        )
        assertTrue(prompt.contains("verdict"))
        assertTrue(prompt.contains("score"))
        assertTrue(prompt.contains("feedback"))
        assertTrue(prompt.contains("missing"))
    }

    @Test
    fun testGemma4SystemInstructionForCollege() {
        val collegeProfile = StudentProfile(
            academicLevel = AcademicLevel.COLLEGE,
            collegeCourse = "BS Computer Science",
            preferredLanguage = LanguageMode.EN
        )
        val instruction = PromptBuilder.buildSystemInstruction(collegeProfile, LanguageMode.EN)
        assertTrue(instruction.contains("BS Computer Science"))
        assertTrue(instruction.contains("English"))
        assertTrue(instruction.contains("JSON Lines"))
        assertTrue(instruction.contains("Ground EVERYTHING"))
    }
}
