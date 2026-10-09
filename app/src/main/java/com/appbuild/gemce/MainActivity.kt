package com.appbuild.gemce

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.DownloadState
import com.appbuild.gemce.ai.EngineStatus
import com.appbuild.gemce.ai.FakeEngine
import com.appbuild.gemce.ai.LiteRtLmEngine
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.ai.ModelDownloader
import com.appbuild.gemce.ai.ModelStorageManager
import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.library.DocumentRepository
import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.model.KeyPointJsonItem
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.model.McqJsonItem
import com.appbuild.gemce.domain.model.ShortAnswerJsonItem
import com.appbuild.gemce.domain.parser.JsonlParser
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.profile.UserProfileRepository
import com.appbuild.gemce.domain.prompt.PromptBuilder
import com.appbuild.gemce.domain.settings.AppSettingsRepository
import com.appbuild.gemce.ui.chat.ContextualChatScreen
import com.appbuild.gemce.ui.ingest.IngestScreen
import com.appbuild.gemce.ui.library.LibraryScreen
import com.appbuild.gemce.ui.profile.OnboardingSetupDialog
import com.appbuild.gemce.ui.settings.SettingsScreen
import com.appbuild.gemce.ui.study.FlashcardPlayer
import com.appbuild.gemce.ui.study.InteractiveTestPlayer
import com.appbuild.gemce.ui.study.McqQuizPlayer
import com.appbuild.gemce.ui.study.ReviewerPlayer
import com.appbuild.gemce.ui.theme.GemceTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize user profile & app settings
        UserProfileRepository.initialize(applicationContext)
        AppSettingsRepository.initialize(applicationContext)

        val fakeEngine = FakeEngine()
        val liteRtLmEngine = LiteRtLmEngine(applicationContext)

        setContent {
            val appSettings by AppSettingsRepository.settings.collectAsState()
            GemceTheme(
                themeMode = appSettings.themeMode,
                fontPreference = appSettings.fontPreference
            ) {
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentNavTab by remember { mutableIntStateOf(0) } // 0: Study Hub, 1: Chat, 2: Library, 3: Import, 4: Model Settings

    val studentProfile by UserProfileRepository.studentProfile.collectAsState()
    var showSetupDialog by remember { mutableStateOf(!studentProfile.isConfigured) }

    // Set Gemma 4 E2B as default when model file is present
    val isModelInstalled = liteRtLmEngine.isModelInstalled()
    var useRealModel by remember(isModelInstalled) { mutableStateOf(isModelInstalled) }
    val currentEngine: LlmEngine = if (useRealModel && isModelInstalled) liteRtLmEngine else fakeEngine
    val engineStatus by currentEngine.status.collectAsState()
    val downloadState by ModelDownloader.downloadState.collectAsState()

    val documents by DocumentRepository.documents.collectAsState()
    var activeSections by remember(documents) {
        mutableStateOf(
            DocumentRepository.getActiveContextSections().ifEmpty {
                documents.firstOrNull()?.sections ?: emptyList()
            }
        )
    }

    var selectedLanguage by remember { mutableStateOf(studentProfile.preferredLanguage) }
    var selectedStudyTemplate by remember { mutableIntStateOf(0) } // 0: Flashcards, 1: Quiz, 2: Interactive Test, 3: Reviewer

    // Generated Study Datasets
    val flashcards = remember { mutableStateListOf<CardJsonItem>() }
    val mcqQuestions = remember { mutableStateListOf<McqJsonItem>() }
    val shortQuestions = remember { mutableStateListOf<ShortAnswerJsonItem>() }
    val keyPoints = remember { mutableStateListOf<KeyPointJsonItem>() }
    var isGenerating by remember { mutableStateOf(false) }

    fun generateForCurrentSection(targetSections: List<Section> = activeSections) {
        val section = targetSections.firstOrNull() ?: return
        coroutineScope.launch {
            isGenerating = true
            try {
                if (!currentEngine.isReady) {
                    currentEngine.initialize()
                }

                // Generate Cards
                flashcards.clear()
                val cardPrompt = PromptBuilder.buildFlashcardPrompt(
                    sectionText = section.text,
                    language = selectedLanguage,
                    profile = studentProfile
                )
                var cardBuffer = ""
                currentEngine.generateStream(cardPrompt).collect { chunk ->
                    cardBuffer += chunk
                    while (cardBuffer.contains("\n")) {
                        val newlineIdx = cardBuffer.indexOf("\n")
                        val line = cardBuffer.substring(0, newlineIdx).trim()
                        cardBuffer = cardBuffer.substring(newlineIdx + 1)
                        if (line.isNotEmpty()) {
                            JsonlParser.parseCard(line)?.let { flashcards.add(it) }
                        }
                    }
                }
                if (cardBuffer.isNotBlank()) {
                    JsonlParser.parseCard(cardBuffer.trim())?.let { flashcards.add(it) }
                }

                // Generate MCQs
                mcqQuestions.clear()
                val mcqPrompt = PromptBuilder.buildMcqPrompt(
                    sectionText = section.text,
                    language = selectedLanguage,
                    profile = studentProfile
                )
                var mcqBuffer = ""
                currentEngine.generateStream(mcqPrompt).collect { chunk ->
                    mcqBuffer += chunk
                    while (mcqBuffer.contains("\n")) {
                        val newlineIdx = mcqBuffer.indexOf("\n")
                        val line = mcqBuffer.substring(0, newlineIdx).trim()
                        mcqBuffer = mcqBuffer.substring(newlineIdx + 1)
                        if (line.isNotEmpty()) {
                            JsonlParser.parseMcq(line)?.let { mcqQuestions.add(it) }
                        }
                    }
                }
                if (mcqBuffer.isNotBlank()) {
                    JsonlParser.parseMcq(mcqBuffer.trim())?.let { mcqQuestions.add(it) }
                }

                // Generate Short-Answer items
                shortQuestions.clear()
                val shortPrompt = PromptBuilder.buildShortAnswerPrompt(
                    sectionText = section.text,
                    language = selectedLanguage,
                    profile = studentProfile
                )
                var shortBuffer = ""
                currentEngine.generateStream(shortPrompt).collect { chunk ->
                    shortBuffer += chunk
                    while (shortBuffer.contains("\n")) {
                        val newlineIdx = shortBuffer.indexOf("\n")
                        val line = shortBuffer.substring(0, newlineIdx).trim()
                        shortBuffer = shortBuffer.substring(newlineIdx + 1)
                        if (line.isNotEmpty()) {
                            JsonlParser.parseShortAnswer(line)?.let { shortQuestions.add(it) }
                        }
                    }
                }
                if (shortBuffer.isNotBlank()) {
                    JsonlParser.parseShortAnswer(shortBuffer.trim())?.let { shortQuestions.add(it) }
                }

                // Generate Reviewer Points
                keyPoints.clear()
                val pointPrompt = "Generate key points for reviewer in ${selectedLanguage.label}:\n" + section.text
                var pointBuffer = ""
                currentEngine.generateStream(pointPrompt).collect { chunk ->
                    pointBuffer += chunk
                    while (pointBuffer.contains("\n")) {
                        val newlineIdx = pointBuffer.indexOf("\n")
                        val line = pointBuffer.substring(0, newlineIdx).trim()
                        pointBuffer = pointBuffer.substring(newlineIdx + 1)
                        if (line.isNotEmpty()) {
                            JsonlParser.parseKeyPoint(line)?.let { keyPoints.add(it) }
                        }
                    }
                }
                if (pointBuffer.isNotBlank()) {
                    JsonlParser.parseKeyPoint(pointBuffer.trim())?.let { keyPoints.add(it) }
                }
            } catch (_: Exception) {
            } finally {
                isGenerating = false
            }
        }
    }

    // Model File Picker Launcher (allows user to link an existing downloaded model file)
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val persistentTarget = ModelStorageManager.getPersistentModelFile()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(persistentTarget).use { output ->
                            input.copyTo(output)
                        }
                    }
                    useRealModel = true
                } catch (_: Exception) {
                }
            }
        }
    }

    if (showSetupDialog) {
        OnboardingSetupDialog(
            initialProfile = studentProfile,
            onSaveProfile = { updated ->
                UserProfileRepository.updateProfile(updated)
                selectedLanguage = updated.preferredLanguage
                showSetupDialog = false
            },
            onDismissRequest = if (studentProfile.isConfigured) {
                { showSetupDialog = false }
            } else null
        )
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
                    label = { Text("Chat") },
                    icon = { Text("💬") }
                )
                NavigationBarItem(
                    selected = currentNavTab == 2,
                    onClick = { currentNavTab = 2 },
                    label = { Text("Library") },
                    icon = { Text("📁") }
                )
                NavigationBarItem(
                    selected = currentNavTab == 3,
                    onClick = { currentNavTab = 3 },
                    label = { Text("Import") },
                    icon = { Text("📥") }
                )
                NavigationBarItem(
                    selected = currentNavTab == 4,
                    onClick = { currentNavTab = 4 },
                    label = { Text("Settings") },
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
            // Persistent Academic Context Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSetupDialog = true }
                    ) {
                        Text(
                            text = "🎓 ${studentProfile.getBadgeText()} • ${selectedLanguage.label}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✎ Edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Language Quick Switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(LanguageMode.EN, LanguageMode.FIL, LanguageMode.TAGLISH, LanguageMode.CEB).forEach { lang ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (selectedLanguage == lang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { selectedLanguage = lang }
                            ) {
                                Text(
                                    text = lang.code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedLanguage == lang) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (currentNavTab) {
                0 -> {
                    // Study Hub
                    Column(modifier = Modifier.fillMaxSize()) {
                        val currentDocTitle = activeSections.firstOrNull()?.documentTitle ?: "No Document Selected"
                        val currentSectionTitle = activeSections.firstOrNull()?.title ?: "Mitochondria & Cellular Energy"

                        // Top Action Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$currentDocTitle • ${if (useRealModel && liteRtLmEngine.isModelInstalled()) "Gemma 4 E2B" else "FakeEngine"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { generateForCurrentSection() },
                                enabled = !isGenerating && activeSections.isNotEmpty()
                            ) {
                                Text(if (isGenerating) "Generating..." else "Generate Study Pack")
                            }
                        }

                        // Study Mode Tabs
                        PrimaryScrollableTabRow(
                            selectedTabIndex = selectedStudyTemplate,
                            edgePadding = 12.dp
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
                        }

                        // Study Template Content
                        when (selectedStudyTemplate) {
                            0 -> FlashcardPlayer(cards = flashcards, onFinish = {})
                            1 -> McqQuizPlayer(questions = mcqQuestions, onFinish = { _, _ -> })
                            2 -> InteractiveTestPlayer(questions = shortQuestions, engine = currentEngine, onFinish = { _, _ -> })
                            3 -> ReviewerPlayer(sectionTitle = currentSectionTitle, keyPoints = keyPoints)
                        }
                    }
                }

                1 -> {
                    // Chat Screen with dynamic student profile and language
                    ContextualChatScreen(
                        engine = currentEngine,
                        language = selectedLanguage,
                        studentProfile = studentProfile
                    )
                }

                2 -> {
                    // Library Screen
                    LibraryScreen(
                        onStudyDocument = { doc ->
                            doc.sections.firstOrNull()?.let { section ->
                                activeSections = listOf(section)
                                currentNavTab = 0
                                generateForCurrentSection(listOf(section))
                            }
                        }
                    )
                }

                3 -> {
                    // Import Screen
                    IngestScreen(
                        onSectionsReady = { sections, lang ->
                            activeSections = sections
                            selectedLanguage = lang
                            currentNavTab = 0
                            generateForCurrentSection(sections)
                        }
                    )
                }

                4 -> {
                    // Consolidated Settings & AI Model Management Screen
                    SettingsScreen(
                        currentEngine = currentEngine,
                        liteRtLmEngine = liteRtLmEngine,
                        useRealModel = useRealModel,
                        onToggleRealModel = { wantReal ->
                            coroutineScope.launch {
                                currentEngine.unload()
                                useRealModel = wantReal
                            }
                        },
                        studentProfile = studentProfile,
                        onEditProfileClick = { showSetupDialog = true },
                        onPickModelFileClick = { modelPickerLauncher.launch("*/*") },
                        context = context
                    )
                }
            }
        }
    }
}
