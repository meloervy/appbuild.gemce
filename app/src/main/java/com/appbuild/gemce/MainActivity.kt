package com.appbuild.gemce

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.appbuild.gemce.domain.chat.ChatSessionRepository
import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.library.DocumentRepository
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.profile.UserProfileRepository
import com.appbuild.gemce.domain.settings.AppSettingsRepository
import com.appbuild.gemce.ui.chat.ContextualChatScreen
import com.appbuild.gemce.ui.ingest.IngestScreen
import com.appbuild.gemce.ui.library.LibraryScreen
import com.appbuild.gemce.ui.profile.OnboardingSetupDialog
import com.appbuild.gemce.ui.settings.SettingsScreen
import com.appbuild.gemce.ui.study.AdaptiveStudyHubScreen
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
        ChatSessionRepository.initialize(applicationContext)

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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentNavTab) {
                            0 -> "Gemce Study Hub"
                            1 -> "Ask Gemce"
                            2 -> "Academic Library"
                            3 -> "Settings & AI Engine"
                            else -> "Gemce"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (engineStatus is EngineStatus.Loading) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = when {
                                engineStatus is EngineStatus.Loading -> "⏳ Initializing..."
                                useRealModel && liteRtLmEngine.isModelInstalled() -> "🤖 Gemma 4 E2B"
                                else -> "⚡ Demo Mode"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (engineStatus is EngineStatus.Loading) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
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
            // Persistent Engine Loading Indicator
            AnimatedVisibility(visible = engineStatus is EngineStatus.Loading) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Gemma 4 E2B is initializing weights into memory... Please wait",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            when (currentNavTab) {
                0 -> {
                    // Adaptive AI Study Hub & Advisor
                    AdaptiveStudyHubScreen(
                        currentEngine = currentEngine,
                        activeSections = activeSections,
                        studentProfile = studentProfile,
                        selectedLanguage = selectedLanguage
                    )
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
                    // Consolidated Academic Library Screen (with + Add Material flow)
                    LibraryScreen(
                        onStudyDocument = { doc ->
                            doc.sections.firstOrNull()?.let { section ->
                                activeSections = listOf(section)
                                currentNavTab = 0
                            }
                        }
                    )
                }

                3 -> {
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
                        onLanguageSelected = { selectedLanguage = it },
                        onPickModelFileClick = { modelPickerLauncher.launch("*/*") },
                        context = context
                    )
                }
            }
        }
    }
}
