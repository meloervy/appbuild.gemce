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
import com.appbuild.gemce.ui.chat.ContextualChatScreen
import com.appbuild.gemce.ui.ingest.IngestScreen
import com.appbuild.gemce.ui.library.LibraryScreen
import com.appbuild.gemce.ui.profile.OnboardingSetupDialog
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

        // Initialize user profile settings
        UserProfileRepository.initialize(applicationContext)

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentNavTab by remember { mutableIntStateOf(0) } // 0: Study Hub, 1: Chat, 2: Library, 3: Import, 4: Model Settings

    val studentProfile by UserProfileRepository.studentProfile.collectAsState()
    var showSetupDialog by remember { mutableStateOf(!studentProfile.isConfigured) }

    // Set Gemma 4 E2B as default, falls back to FakeEngine if model file not yet downloaded
    val isModelInstalled = remember { liteRtLmEngine.isModelInstalled() }
    var useRealModel by remember { mutableStateOf(isModelInstalled) }
    val currentEngine: LlmEngine = if (useRealModel && liteRtLmEngine.isModelInstalled()) liteRtLmEngine else fakeEngine
    val engineStatus by currentEngine.status.collectAsState()
    val downloadState by ModelDownloader.downloadState.collectAsState()

    val documents by DocumentRepository.documents.collectAsState()
    var activeSections by remember {
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
            currentEngine.generateStream(cardPrompt).collect { line ->
                JsonlParser.parseCard(line)?.let { flashcards.add(it) }
            }

            // Generate MCQs
            mcqQuestions.clear()
            val mcqPrompt = PromptBuilder.buildMcqPrompt(
                sectionText = section.text,
                language = selectedLanguage,
                profile = studentProfile
            )
            currentEngine.generateStream(mcqPrompt).collect { line ->
                JsonlParser.parseMcq(line)?.let { mcqQuestions.add(it) }
            }

            // Generate Short-Answer items
            shortQuestions.clear()
            val shortPrompt = PromptBuilder.buildShortAnswerPrompt(
                sectionText = section.text,
                language = selectedLanguage,
                profile = studentProfile
            )
            currentEngine.generateStream(shortPrompt).collect { line ->
                JsonlParser.parseShortAnswer(line)?.let { shortQuestions.add(it) }
            }

            // Generate Reviewer Points
            keyPoints.clear()
            val pointPrompt = "Generate key points for reviewer in ${selectedLanguage.label}:\n" + section.text
            currentEngine.generateStream(pointPrompt).collect { line ->
                JsonlParser.parseKeyPoint(line)?.let { keyPoints.add(it) }
            }

            isGenerating = false
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
                    // Model Management Screen (Persistent Storage Details)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Gemma 4 E2B AI Model",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Offline on-device intelligence. The model file is stored in persistent shared storage so it remains intact even if Gemce is updated or uninstalled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Model File: ${ModelStorageManager.MODEL_FILENAME} (2.41 GB)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = liteRtLmEngine.getStorageDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (liteRtLmEngine.isModelInstalled()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                when (val state = downloadState) {
                                    is DownloadState.Idle -> {
                                        if (!liteRtLmEngine.isModelInstalled()) {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        ModelDownloader.startDownload(context)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Download Gemma 4 E2B to Persistent Storage")
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedButton(
                                                onClick = {
                                                    modelPickerLauncher.launch("*/*")
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Locate / Link Existing Model File")
                                            }
                                        } else {
                                            Text(
                                                text = "✓ Model is ready and secured in persistent storage.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    is DownloadState.Downloading -> {
                                        LinearProgressIndicator(
                                            progress = { state.progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${(state.progress * 100).toInt()}% (${state.bytesDownloaded / (1024 * 1024)} MB / 2410 MB)",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = "${"%.1f".format(state.speedMbPerSec)} MB/s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { ModelDownloader.cancelDownload() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Cancel Download")
                                        }
                                    }
                                    is DownloadState.Completed -> {
                                        Text(
                                            text = "Download complete! Stored in persistent Downloads directory.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    is DownloadState.Error -> {
                                        Text(
                                            text = "Download error: ${state.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    ModelDownloader.startDownload(context)
                                                }
                                            }
                                        ) {
                                            Text("Retry Download")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Engine Mode Switcher
                        Text(text = "Active AI Engine:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = useRealModel,
                                onClick = {
                                    coroutineScope.launch {
                                        currentEngine.unload()
                                        useRealModel = true
                                    }
                                },
                                label = { Text("Gemma 4 E2B (Default)") }
                            )
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
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Status: ${
                                        when (val s = engineStatus) {
                                            is EngineStatus.Ready -> "Ready for on-device inference"
                                            is EngineStatus.Loading -> "Loading: ${s.message}"
                                            is EngineStatus.Unloaded -> "Unloaded"
                                            is EngineStatus.Error -> "Error: ${s.throwable.message}"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ADB Persistent Push Command:\nadb push gemma-4-E2B-it.litertlm /sdcard/Download/Gemce/models/\n\n(Files in /sdcard/Download/ are NEVER erased when uninstalling or updating the app.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
