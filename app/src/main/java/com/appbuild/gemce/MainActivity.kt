package com.appbuild.gemce

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.EngineStatus
import com.appbuild.gemce.ai.FakeEngine
import com.appbuild.gemce.ai.LiteRtLmEngine
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.ingest.SectionChunker
import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.model.KeyPointJsonItem
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.model.McqJsonItem
import com.appbuild.gemce.domain.model.ShortAnswerJsonItem
import com.appbuild.gemce.domain.parser.JsonlParser
import com.appbuild.gemce.domain.prompt.PromptBuilder
import com.appbuild.gemce.ui.ingest.IngestScreen
import com.appbuild.gemce.ui.study.AskGemceChat
import com.appbuild.gemce.ui.study.FlashcardPlayer
import com.appbuild.gemce.ui.study.InteractiveTestPlayer
import com.appbuild.gemce.ui.study.McqQuizPlayer
import com.appbuild.gemce.ui.study.ReviewerPlayer
import com.appbuild.gemce.ui.theme.GemceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fakeEngine = FakeEngine()
        val liteRtLmEngine = LiteRtLmEngine(applicationContext)

        setContent {
            GemceTheme {
                MainAppScaffold(
                    fakeEngine = fakeEngine,
                    liteRtLmEngine = liteRtLmEngine
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    fakeEngine: FakeEngine,
    liteRtLmEngine: LiteRtLmEngine
) {
    val coroutineScope = rememberCoroutineScope()
    var currentNavTab by remember { mutableIntStateOf(0) } // 0: Study Hub, 1: Import, 2: Model Setup

    var useRealModel by remember { mutableStateOf(false) }
    val currentEngine: LlmEngine = if (useRealModel) liteRtLmEngine else fakeEngine
    val engineStatus by currentEngine.status.collectAsState()

    var activeSections by remember {
        mutableStateOf(
            SectionChunker.chunk(
                documentTitle = "Cell Biology & Energy",
                rawText = """Mitochondria are membrane-bound cell organelles that generate most of the chemical energy needed to power the cell's biochemical reactions. Chemical energy produced by the mitochondria is stored in a small molecule called adenosine triphosphate (ATP).

Mitochondria produce ATP through cellular respiration, converting glucose and oxygen into energy, carbon dioxide, and water. Because ATP is used by cells as universal energy currency, mitochondria are commonly referred to as the powerhouses of the cell."""
            )
        )
    }

    var selectedLanguage by remember { mutableStateOf(LanguageMode.TAGLISH) }
    var selectedStudyTemplate by remember { mutableIntStateOf(0) } // 0: Flashcards, 1: Quiz, 2: Interactive Test, 3: Reviewer, 4: Ask Gemce

    // Generated Study Datasets
    val flashcards = remember { mutableStateListOf<CardJsonItem>() }
    val mcqQuestions = remember { mutableStateListOf<McqJsonItem>() }
    val shortQuestions = remember { mutableStateListOf<ShortAnswerJsonItem>() }
    val keyPoints = remember { mutableStateListOf<KeyPointJsonItem>() }
    var isGenerating by remember { mutableStateOf(false) }

    fun generateForCurrentSection() {
        val section = activeSections.firstOrNull() ?: return
        coroutineScope.launch {
            isGenerating = true
            if (!currentEngine.isReady) {
                currentEngine.initialize()
            }

            // Generate Cards
            flashcards.clear()
            val cardPrompt = PromptBuilder.buildFlashcardPrompt(section.text, language = selectedLanguage)
            currentEngine.generateStream(cardPrompt).collect { line ->
                JsonlParser.parseCard(line)?.let { flashcards.add(it) }
            }

            // Generate MCQs
            mcqQuestions.clear()
            val mcqPrompt = PromptBuilder.buildMcqPrompt(section.text, language = selectedLanguage)
            currentEngine.generateStream(mcqPrompt).collect { line ->
                JsonlParser.parseMcq(line)?.let { mcqQuestions.add(it) }
            }

            // Generate Short-Answer items
            shortQuestions.clear()
            val shortPrompt = PromptBuilder.buildShortAnswerPrompt(section.text, language = selectedLanguage)
            currentEngine.generateStream(shortPrompt).collect { line ->
                JsonlParser.parseShortAnswer(line)?.let { shortQuestions.add(it) }
            }

            // Generate Reviewer Points
            keyPoints.clear()
            val pointPrompt = "Generate key points for reviewer:\n" + section.text
            currentEngine.generateStream(pointPrompt).collect { line ->
                JsonlParser.parseKeyPoint(line)?.let { keyPoints.add(it) }
            }

            isGenerating = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentNavTab == 0,
                    onClick = { currentNavTab = 0 },
                    label = { Text("Study Hub") },
                    icon = { Text("📚") }
                )
                NavigationBarItem(
                    selected = currentNavTab == 1,
                    onClick = { currentNavTab = 1 },
                    label = { Text("Import") },
                    icon = { Text("📥") }
                )
                NavigationBarItem(
                    selected = currentNavTab == 2,
                    onClick = { currentNavTab = 2 },
                    label = { Text("Model") },
                    icon = { Text("⚙️") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNavTab) {
                0 -> {
                    // STUDY HUB
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header with generation action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = activeSections.firstOrNull()?.documentTitle ?: "Gemce Study",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Language: ${selectedLanguage.label} | ${if (useRealModel) "Gemma 4 E2B" else "FakeEngine"}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Button(
                                onClick = { generateForCurrentSection() },
                                enabled = !isGenerating && activeSections.isNotEmpty()
                            ) {
                                Text(if (isGenerating) "Generating..." else "Generate Study Pack")
                            }
                        }

                        if (isGenerating) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Study Template Selector Tabs
                        ScrollableTabRow(
                            selectedTabIndex = selectedStudyTemplate,
                            edgePadding = 16.dp
                        ) {
                            Tab(
                                selected = selectedStudyTemplate == 0,
                                onClick = { selectedStudyTemplate = 0 },
                                text = { Text("Cards (${flashcards.size})") }
                            )
                            Tab(
                                selected = selectedStudyTemplate == 1,
                                onClick = { selectedStudyTemplate = 1 },
                                text = { Text("MCQ Quiz (${mcqQuestions.size})") }
                            )
                            Tab(
                                selected = selectedStudyTemplate == 2,
                                onClick = { selectedStudyTemplate = 2 },
                                text = { Text("Interactive Test (${shortQuestions.size})") }
                            )
                            Tab(
                                selected = selectedStudyTemplate == 3,
                                onClick = { selectedStudyTemplate = 3 },
                                text = { Text("Reviewer (${keyPoints.size})") }
                            )
                            Tab(
                                selected = selectedStudyTemplate == 4,
                                onClick = { selectedStudyTemplate = 4 },
                                text = { Text("Ask Gemce") }
                            )
                        }

                        // Render Active Dynamic Template
                        when (selectedStudyTemplate) {
                            0 -> FlashcardPlayer(
                                cards = flashcards,
                                onFinish = { selectedStudyTemplate = 1 }
                            )
                            1 -> McqQuizPlayer(
                                questions = mcqQuestions,
                                onFinish = { _, _ -> selectedStudyTemplate = 2 }
                            )
                            2 -> InteractiveTestPlayer(
                                questions = shortQuestions,
                                engine = currentEngine,
                                onFinish = { _, _ -> selectedStudyTemplate = 3 }
                            )
                            3 -> ReviewerPlayer(
                                sectionTitle = activeSections.firstOrNull()?.title ?: "Overview",
                                keyPoints = keyPoints
                            )
                            4 -> AskGemceChat(
                                sections = activeSections,
                                engine = currentEngine
                            )
                        }
                    }
                }

                1 -> {
                    // IMPORT NOTES
                    IngestScreen(
                        onSectionsReady = { sections, language ->
                            activeSections = sections
                            selectedLanguage = language
                            currentNavTab = 0
                            generateForCurrentSection()
                        }
                    )
                }

                2 -> {
                    // MODEL & OFFLINE SETTINGS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Model Settings (Offline-First)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gemma 4 E2B via LiteRT-LM. No internet connection required after setup.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !useRealModel,
                                onClick = {
                                    coroutineScope.launch {
                                        currentEngine.unload()
                                        useRealModel = false
                                    }
                                },
                                label = { Text("FakeEngine (Instant Testing)") }
                            )
                            FilterChip(
                                selected = useRealModel,
                                onClick = {
                                    coroutineScope.launch {
                                        currentEngine.unload()
                                        useRealModel = true
                                    }
                                },
                                label = { Text("Gemma 4 E2B (On-Device)") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Active Engine: ${if (useRealModel) "LiteRT-LM Gemma 4 E2B" else "FakeEngine"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Status: ${
                                        when (val s = engineStatus) {
                                            is EngineStatus.Ready -> "Ready"
                                            is EngineStatus.Loading -> "Loading: ${s.message}"
                                            is EngineStatus.Unloaded -> "Unloaded"
                                            is EngineStatus.Error -> "Error: ${s.throwable.message}"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (liteRtLmEngine.isModelInstalled()) {
                                        "Model installed: ${liteRtLmEngine.modelFile.absolutePath}"
                                    } else {
                                        "Model file not found. To install on your phone, push the model file:\n\nadb push gemma-4-E2B-it.litertlm /data/local/tmp/\nadb shell run-as com.appbuild.gemce mkdir -p files/models\nadb shell run-as com.appbuild.gemce cp /data/local/tmp/gemma-4-E2B-it.litertlm files/models/"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (liteRtLmEngine.isModelInstalled()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    currentEngine.unload()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unload Model (Free RAM)")
                        }
                    }
                }
            }
        }
    }
}
