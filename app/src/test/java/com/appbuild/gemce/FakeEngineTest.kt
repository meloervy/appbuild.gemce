package com.appbuild.gemce

import com.appbuild.gemce.ai.FakeEngine
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.parser.JsonlParser
import com.appbuild.gemce.domain.profile.AcademicLevel
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.prompt.PromptBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeEngineTest {

    private val engine = FakeEngine(simulatedDelayMs = 0L)

    @Test
    fun testRizalTopicProducesRizalFlashcards() = runBlocking {
        val prompt = PromptBuilder.buildFlashcardPrompt(
            sectionText = "Comprehensive academic subject: Life and Works of Rizal. Core principles of Rizal's novels and nationalism.",
            language = LanguageMode.EN
        )
        val outputs = engine.generateStream(prompt).toList().joinToString("")
        val parsed = JsonlParser.parseCard(outputs.lines().first { it.isNotBlank() })

        assertNotNull(parsed)
        assertTrue(outputs.contains("Noli") || outputs.contains("Rizal") || outputs.contains("Gomburza") || outputs.contains("Dapitan"))
        assertFalse(outputs.contains("Mitochondria"))
        assertFalse(outputs.contains("ATP"))
    }

    @Test
    fun testDiscreteMathTopicProducesDiscreteMathMcq() = runBlocking {
        val prompt = PromptBuilder.buildMcqPrompt(
            sectionText = "Topic: Discrete Mathematics. Graph theory, pigeonhole principle, set theory.",
            language = LanguageMode.EN
        )
        val outputs = engine.generateStream(prompt).toList().joinToString("")
        val parsed = JsonlParser.parseMcq(outputs.lines().first { it.isNotBlank() })

        assertNotNull(parsed)
        assertTrue(outputs.contains("Pigeonhole") || outputs.contains("power set") || outputs.contains("Disjunction") || outputs.contains("graph"))
        assertFalse(outputs.contains("Mitochondria"))
    }

    @Test
    fun testEmergingTechTopicProducesEmergingTechReviewer() = runBlocking {
        val prompt = "Generate key points for reviewer in English:\nTopic: Emerging Technologies. Quantum computing, edge IoT, generative AI, blockchain."
        val outputs = engine.generateStream(prompt).toList().joinToString("")
        val parsed = JsonlParser.parseKeyPoint(outputs.lines().first { it.isNotBlank() })

        assertNotNull(parsed)
        assertTrue(outputs.contains("Quantum") || outputs.contains("Edge") || outputs.contains("Blockchain") || outputs.contains("Transformer"))
        assertFalse(outputs.contains("Mitochondria"))
    }

    @Test
    fun testCustomNotesAreGroundedInOutputCards() = runBlocking {
        val customNote = "The Philippine Bill of Rights protects citizens against unreasonable searches and seizures without a valid warrant."
        val prompt = PromptBuilder.buildFlashcardPrompt(
            sectionText = customNote,
            language = LanguageMode.EN
        )
        val outputs = engine.generateStream(prompt).toList().joinToString("")
        val parsed = JsonlParser.parseCard(outputs.lines().first { it.isNotBlank() })

        assertNotNull(parsed)
        assertTrue(outputs.contains("searches and seizures") || outputs.contains("warrant") || outputs.contains("Philippine Bill of Rights"))
        assertFalse(outputs.contains("Mitochondria"))
    }

    @Test
    fun testAskGemceChatTopicNeutrality() = runBlocking {
        val prompt = PromptBuilder.buildAskGemcePrompt(
            sectionsWithLabels = emptyList(),
            question = "Tell me about Jose Rizal's exile in Dapitan",
            language = LanguageMode.EN
        )
        val outputs = engine.generateStream(prompt).toList().joinToString("")

        assertTrue(outputs.contains("Rizal") || outputs.contains("Dapitan"))
        assertFalse(outputs.contains("System Design & Architecture Overview"))
    }

    @Test
    fun testAskGemceChatDiscreteMath() = runBlocking {
        val prompt = PromptBuilder.buildAskGemcePrompt(
            sectionsWithLabels = emptyList(),
            question = "Explain the Pigeonhole Principle in discrete mathematics",
            language = LanguageMode.EN
        )
        val outputs = engine.generateStream(prompt).toList().joinToString("")

        assertTrue(outputs.contains("Discrete") || outputs.contains("Pigeonhole"))
        assertFalse(outputs.contains("System Design & Architecture Overview"))
    }
}
