package com.appbuild.gemce.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.model.KeyPointJsonItem
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.model.McqJsonItem
import com.appbuild.gemce.domain.model.ShortAnswerJsonItem
import com.appbuild.gemce.domain.parser.JsonlParser
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.prompt.PromptBuilder
import kotlinx.coroutines.launch

enum class StudyGoal(val title: String, val subtitle: String, val icon: String, val recommendedMethodIndex: Int) {
    MEMORY("Rapid Recall & Formulas", "Active recall for definitions, formulas & terms", "⚡", 0),
    QUIZ("Exam Practice & Quizzes", "Multiple-choice questions with instant scoring", "🎯", 1),
    DEEP_TEST("Deep Problem Solving", "Short-answer essay questions evaluated by AI", "🧠", 2),
    SUMMARY("Comprehensive Reviewer", "Structured high-yield concept summary", "📖", 3)
}

enum class FamiliarityLevel(val label: String, val icon: String) {
    BEGINNER("Beginner", "🌱"),
    INTERMEDIATE("Intermediate", "🌿"),
    ADVANCED("Advanced", "🌳")
}

@Composable
fun AdaptiveStudyHubScreen(
    currentEngine: LlmEngine,
    activeSections: List<Section>,
    studentProfile: StudentProfile,
    selectedLanguage: LanguageMode,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var studySessionActive by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf(StudyGoal.MEMORY) }
    var familiarity by remember { mutableStateOf(FamiliarityLevel.INTERMEDIATE) }

    val defaultTopic = activeSections.firstOrNull()?.title ?: ""
    var customTopicInput by remember(defaultTopic) { mutableStateOf(defaultTopic) }

    // Generated datasets
    val flashcards = remember { mutableStateListOf<CardJsonItem>() }
    val mcqQuestions = remember { mutableStateListOf<McqJsonItem>() }
    val shortQuestions = remember { mutableStateListOf<ShortAnswerJsonItem>() }
    val keyPoints = remember { mutableStateListOf<KeyPointJsonItem>() }
    var isGenerating by remember { mutableStateOf(false) }

    fun generateAndStart(goal: StudyGoal) {
        val topic = customTopicInput.ifBlank { activeSections.firstOrNull()?.title ?: "Earth and Life Science" }
        val matchingSection = activeSections.firstOrNull {
            it.title.contains(topic, ignoreCase = true) || topic.contains(it.title, ignoreCase = true)
        }
        val sectionContent = resolveTopicContent(topic, matchingSection, activeSections)

        coroutineScope.launch {
            isGenerating = true
            studySessionActive = true
            try {
                if (!currentEngine.isReady) {
                    currentEngine.initialize()
                }

                when (goal.recommendedMethodIndex) {
                    0 -> {
                        // Flashcards
                        flashcards.clear()
                        val prompt = PromptBuilder.buildFlashcardPrompt(
                            sectionText = sectionContent,
                            language = selectedLanguage,
                            profile = studentProfile
                        )
                        var buffer = ""
                        currentEngine.generateStream(prompt).collect { chunk ->
                            buffer += chunk
                            while (buffer.contains("\n")) {
                                val newlineIdx = buffer.indexOf("\n")
                                val line = buffer.substring(0, newlineIdx).trim()
                                buffer = buffer.substring(newlineIdx + 1)
                                if (line.isNotEmpty()) {
                                    JsonlParser.parseCard(line)?.let { flashcards.add(it) }
                                }
                            }
                        }
                        if (buffer.isNotBlank()) {
                            JsonlParser.parseCard(buffer.trim())?.let { flashcards.add(it) }
                        }
                    }

                    1 -> {
                        // MCQ Quiz
                        mcqQuestions.clear()
                        val prompt = PromptBuilder.buildMcqPrompt(
                            sectionText = sectionContent,
                            language = selectedLanguage,
                            profile = studentProfile
                        )
                        var buffer = ""
                        currentEngine.generateStream(prompt).collect { chunk ->
                            buffer += chunk
                            while (buffer.contains("\n")) {
                                val newlineIdx = buffer.indexOf("\n")
                                val line = buffer.substring(0, newlineIdx).trim()
                                buffer = buffer.substring(newlineIdx + 1)
                                if (line.isNotEmpty()) {
                                    JsonlParser.parseMcq(line)?.let { mcqQuestions.add(it) }
                                }
                            }
                        }
                        if (buffer.isNotBlank()) {
                            JsonlParser.parseMcq(buffer.trim())?.let { mcqQuestions.add(it) }
                        }
                    }

                    2 -> {
                        // Interactive Test
                        shortQuestions.clear()
                        val prompt = PromptBuilder.buildShortAnswerPrompt(
                            sectionText = sectionContent,
                            language = selectedLanguage,
                            profile = studentProfile
                        )
                        var buffer = ""
                        currentEngine.generateStream(prompt).collect { chunk ->
                            buffer += chunk
                            while (buffer.contains("\n")) {
                                val newlineIdx = buffer.indexOf("\n")
                                val line = buffer.substring(0, newlineIdx).trim()
                                buffer = buffer.substring(newlineIdx + 1)
                                if (line.isNotEmpty()) {
                                    JsonlParser.parseShortAnswer(line)?.let { shortQuestions.add(it) }
                                }
                            }
                        }
                        if (buffer.isNotBlank()) {
                            JsonlParser.parseShortAnswer(buffer.trim())?.let { shortQuestions.add(it) }
                        }
                    }

                    3 -> {
                        // Reviewer
                        keyPoints.clear()
                        val prompt = PromptBuilder.buildReviewerPrompt(
                            sectionText = sectionContent,
                            language = selectedLanguage,
                            profile = studentProfile
                        )
                        var buffer = ""
                        currentEngine.generateStream(prompt).collect { chunk ->
                            buffer += chunk
                            while (buffer.contains("\n")) {
                                val newlineIdx = buffer.indexOf("\n")
                                val line = buffer.substring(0, newlineIdx).trim()
                                buffer = buffer.substring(newlineIdx + 1)
                                if (line.isNotEmpty()) {
                                    JsonlParser.parseKeyPoint(line)?.let { keyPoints.add(it) }
                                }
                            }
                        }
                        if (buffer.isNotBlank()) {
                            JsonlParser.parseKeyPoint(buffer.trim())?.let { keyPoints.add(it) }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                isGenerating = false
            }
        }
    }

    if (!studySessionActive) {
        // STEP 1: AI Study Advisor & Assessment Screen
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Advisor Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🎯 AI Study Advisor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tell Gemce what you want to learn today. We'll diagnose the best study technique tailored to your learning goal across any subject or imported notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Topic Input Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "1. What topic do you need to study?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTopicInput,
                        onValueChange = { customTopicInput = it },
                        placeholder = { Text("e.g. Earth and Life Science, Discrete Math, Rizal, Biology...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Quick topic chips if notes or suggestions exist
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Suggestions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val topicPills = if (activeSections.isNotEmpty()) {
                            activeSections.map { it.title }
                        } else {
                            listOf(
                                "Earth and Life Science",
                                "Discrete Mathematics",
                                "Life & Works of Rizal",
                                "Emerging Technologies",
                                "World History & Civilizations",
                                "General Biology & Ecology",
                                "Ethics & Moral Philosophy"
                            )
                        }
                        topicPills.forEach { pill ->
                            SuggestionChip(
                                onClick = { customTopicInput = pill },
                                label = { Text(pill, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // Study Goal Selector
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "2. What is your primary study objective?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudyGoal.values().forEach { goal ->
                            val isSelected = selectedGoal == goal
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGoal = goal },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = goal.icon, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = goal.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Text("✓", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Familiarity Level
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "3. Current familiarity with this topic:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FamiliarityLevel.values().forEach { level ->
                            FilterChip(
                                selected = familiarity == level,
                                onClick = { familiarity = level },
                                label = { Text("${level.icon} ${level.label}") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Recommendation & Launch Action
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val recommendedName = when (selectedGoal.recommendedMethodIndex) {
                        0 -> "Spaced Repetition Flashcards"
                        1 -> "Adaptive MCQ Quiz"
                        2 -> "Interactive Test (AI Graded)"
                        else -> "Comprehensive Concept Reviewer"
                    }
                    Text(
                        text = "💡 Recommended: $recommendedName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Best fit for ${selectedGoal.title.lowercase()} at ${familiarity.label.lowercase()} level.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { generateAndStart(selectedGoal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("🚀 Generate & Begin Study Session", fontWeight = FontWeight.Bold)
                    }

                    if (selectedGoal == StudyGoal.MEMORY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                flashcards.clear()
                                studySessionActive = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("➕ Create Deck Manually (Zero AI)", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    } else {
        // STEP 2: Active Study Session View
        Column(modifier = modifier.fillMaxSize()) {
            val activeTopicName = customTopicInput.ifBlank { "Study Session" }

            // Active Session Sub-Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeTopicName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${selectedGoal.icon} ${selectedGoal.title} • ${selectedLanguage.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { studySessionActive = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Change Mode", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Gemma 4 E2B is generating your study pack...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calibrating questions to ${studentProfile.getBadgeText()} in ${selectedLanguage.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (selectedGoal.recommendedMethodIndex) {
                    0 -> FlashcardPlayer(
                        cards = flashcards,
                        onFinish = { studySessionActive = false },
                        engine = currentEngine,
                        language = selectedLanguage,
                        studentProfile = studentProfile,
                        onAddCard = { flashcards.add(it) }
                    )
                    1 -> McqQuizPlayer(questions = mcqQuestions, onFinish = { _, _ -> studySessionActive = false })
                    2 -> InteractiveTestPlayer(questions = shortQuestions, engine = currentEngine, onFinish = { _, _ -> studySessionActive = false })
                    3 -> ReviewerPlayer(
                        sectionTitle = activeTopicName,
                        keyPoints = keyPoints,
                        engine = currentEngine,
                        language = selectedLanguage,
                        studentProfile = studentProfile
                    )
                }
            }
        }
    }
}

private fun resolveTopicContent(topic: String, matchingSection: Section?, activeSections: List<Section>): String {
    if (matchingSection != null) return matchingSection.text
    val t = topic.lowercase()
    return when {
        t.contains("earth") || t.contains("life science") || t.contains("lief science") ||
                t.contains("geology") || t.contains("rock") || t.contains("mineral") ||
                t.contains("biosphere") || t.contains("geosphere") || t.contains("hydrosphere") -> """
            Earth and Life Science is an integrated academic discipline exploring terrestrial processes and biological systems.
            The four interconnected subsystems of Earth are the Geosphere (solid rocks and geological interior), the Hydrosphere (all liquid and frozen waters), the Atmosphere (gaseous envelope protecting life), and the Biosphere (all living organisms).
            Earth Science comprises four primary branches: Geology (study of Earth materials, structures, and history), Meteorology (study of atmosphere and weather), Oceanography (study of marine systems), and Astronomy (cosmic context of Earth).
            Rocks are classified into three major groups in the rock cycle: Igneous rocks (crystallized from cooling magma or lava), Sedimentary rocks (formed by deposition, compaction, and cementation of mineral particles), and Metamorphic rocks (transformed by intense heat and pressure).
            Minerals are naturally occurring, inorganic solids with definite chemical composition and crystalline structure, identified by hardness (Mohs scale), streak color, luster reflection, and cleavage planes.
            Plate Tectonics drives continental movement, volcanic activity, and earthquakes through mantle convection currents along divergent, convergent, and transform boundaries.
            Life Science encompasses the origin of life, cellular structure, genetic continuity through DNA and RNA, metabolic energy transfer, and ecological biodiversity across terrestrial and aquatic ecosystems.
        """.trimIndent()

        t.contains("rizal") || t.contains("noli") || t.contains("fili") -> """
            The Life and Works of Jose Rizal examines Dr. Jose Rizal's contributions to Philippine nationalism and literature.
            Rizal authored Noli Me Tangere in Berlin (1887) to expose the social cancer and friar abuses in colonial Philippines.
            His sequel El Filibusterismo was published in Ghent (1891) and dedicated to the martyred priests Gomburza (Gomez, Burgos, Zamora).
            Rizal founded La Liga Filipina in Tondo (1892) advocating civic unity and educational reform before his four-year exile in Dapitan.
            He was executed at Bagumbayan on December 30, 1896, leaving his final poem Mi Último Adiós as a testament to freedom.
        """.trimIndent()

        t.contains("discrete") || t.contains("logic") || t.contains("graph") || t.contains("combinatorics") -> """
            Discrete Mathematics studies mathematical structures that are fundamentally discrete rather than continuous.
            Propositional logic evaluates truth values of compound statements using connectives NOT, AND, OR, conditional implications, and biconditionals.
            Proof techniques include direct proof, proof by contradiction, contrapositive, and Mathematical Induction consisting of the Base Step and Inductive Step.
            The Pigeonhole Principle states that if n items are placed into k containers and n > k, at least one container must contain two or more items.
            The Power Set P(S) of a finite set with n elements has cardinality 2^n.
            In Graph Theory, an Eulerian path traverses every edge of a graph exactly once, while a Hamiltonian path visits every vertex once.
        """.trimIndent()

        t.contains("emerging") || t.contains("quantum") || t.contains("blockchain") || t.contains("ai") || t.contains("transformer") -> """
            Emerging Technologies encompass transformative innovations shaping modern computation and society.
            Quantum computing leverages qubits that exist in quantum superposition (|0> and |1> states simultaneously) and quantum entanglement.
            Blockchain technology utilizes decentralized consensus mechanisms and cryptographic hashing to maintain immutable ledgers without central authority.
            Modern artificial intelligence and Large Language Models rely on Transformer architectures driven by multi-head Self-Attention mechanisms processing token sequences in parallel.
            Edge computing processes data locally near physical sensors and end-user devices to minimize latency and bandwidth consumption.
        """.trimIndent()

        activeSections.isNotEmpty() -> activeSections.first().text

        else -> """
            Academic Subject: $topic.
            Core definitions, foundational principles, scientific or analytical classifications, operational mechanisms, and empirical applications of $topic.
            Systematic analysis of $topic connects fundamental theoretical concepts with verifiable evidence and problem-solving methodologies.
        """.trimIndent()
    }
}
