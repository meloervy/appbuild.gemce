package com.appbuild.gemce.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.ai.DownloadState
import com.appbuild.gemce.ai.EngineStatus
import com.appbuild.gemce.ai.LiteRtLmEngine
import com.appbuild.gemce.ai.LlmEngine
import com.appbuild.gemce.ai.ModelDownloader
import com.appbuild.gemce.ai.ModelStorageManager
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.StudentProfile
import com.appbuild.gemce.domain.settings.AppSettings
import com.appbuild.gemce.domain.settings.AppSettingsRepository
import com.appbuild.gemce.domain.settings.FontPreference
import com.appbuild.gemce.domain.settings.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentEngine: LlmEngine,
    liteRtLmEngine: LiteRtLmEngine,
    useRealModel: Boolean,
    onToggleRealModel: (Boolean) -> Unit,
    studentProfile: StudentProfile,
    onEditProfileClick: () -> Unit,
    onPickModelFileClick: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val appSettings by AppSettingsRepository.settings.collectAsState()
    val engineStatus by currentEngine.status.collectAsState()
    val downloadState by ModelDownloader.downloadState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Column {
            Text(
                text = "Settings & AI Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Customize theme, typography fonts, academic context, and manage on-device Gemma 4 E2B AI model.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Section 1: Appearance & Typography
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎨 Appearance & Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Theme Mode Selector
                Text(text = "Theme Mode:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = appSettings.themeMode == mode,
                            onClick = { AppSettingsRepository.updateThemeMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // Font Preference Selector
                Text(text = "Study Font & Typography:", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Applies across all study cards, quizzes, reviewers, and chats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FontPreference.values().forEach { fontPref ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (appSettings.fontPreference == fontPref) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { AppSettingsRepository.updateFontPreference(fontPref) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontPref.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = fontPref.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (appSettings.fontPreference == fontPref) {
                                    Text(
                                        text = "Active ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Academic Profile & Context
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎓 Student Academic Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(onClick = onEditProfileClick) {
                        Text("Edit Profile")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current Level: ${studentProfile.getFullAcademicContext()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Preferred Language: ${studentProfile.preferredLanguage.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 3: On-Device AI Engine & Model Management (Consolidated)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🤖 Gemma 4 E2B AI Model Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Runs 100% offline via LiteRT-LM. The model is saved to public storage so it persists even if the app is updated or uninstalled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Engine Switcher
                Text(text = "Active Engine:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = useRealModel,
                        onClick = { onToggleRealModel(true) },
                        label = { Text("Gemma 4 E2B") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !useRealModel,
                        onClick = { onToggleRealModel(false) },
                        label = { Text("FakeEngine (Preview)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storage location details
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ModelStorageManager.MODEL_FILENAME,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (liteRtLmEngine.isModelInstalled()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = if (liteRtLmEngine.isModelInstalled()) "Installed ✓" else "Not Installed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (liteRtLmEngine.isModelInstalled()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = liteRtLmEngine.getStorageDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Download / Action Buttons
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
                                Text("Download Gemma 4 E2B (2.41 GB)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onPickModelFileClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Locate / Link Existing Model File")
                            }
                        } else {
                            Text(
                                text = "Model file is ready and secured in persistent storage.",
                                style = MaterialTheme.typography.bodySmall,
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

                Spacer(modifier = Modifier.height(10.dp))

                // Engine status card
                Text(
                    text = "Status: ${
                        when (val s = engineStatus) {
                            is EngineStatus.Ready -> "Ready for on-device inference"
                            is EngineStatus.Loading -> "Loading: ${s.message}"
                            is EngineStatus.Unloaded -> "Unloaded"
                            is EngineStatus.Error -> "Error: ${s.throwable.message}"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ADB Persistent Push Command:\nadb push gemma-4-E2B-it.litertlm /sdcard/Download/Gemce/models/\n\n(Files in /sdcard/Download/ are NEVER erased when uninstalling or updating the app.)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 4: Privacy & Offline Guarantee
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Privacy & Offline Guarantee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• 100% Offline: Notes, questions, flashcards, and answers remain entirely on your device.\n• No accounts, zero analytics, no cloud telemetry.\n• Model download is the only network request ever performed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gemce v0.2 • Target: Android 14 • On-Device Gemma 4 E2B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
