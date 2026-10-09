package com.appbuild.gemce.ui.ingest

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.domain.ingest.OcrHelper
import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.ingest.SectionChunker
import com.appbuild.gemce.domain.model.LanguageMode
import kotlinx.coroutines.launch

@Composable
fun IngestScreen(
    onSectionsReady: (sections: List<Section>, language: LanguageMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Paste, 1: Scan Photo
    var documentTitle by remember { mutableStateOf("Cell Biology & Energy") }
    var rawText by remember {
        mutableStateOf(
            """Mitochondria are membrane-bound cell organelles that generate most of the chemical energy needed to power the cell's biochemical reactions. Chemical energy produced by the mitochondria is stored in a small molecule called adenosine triphosphate (ATP). 

Mitochondria produce ATP through cellular respiration, converting glucose and oxygen into energy, carbon dioxide, and water. Because ATP is used by cells as universal energy currency, mitochondria are commonly referred to as the powerhouses of the cell."""
        )
    }

    var selectedLanguage by remember { mutableStateOf(LanguageMode.TAGLISH) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isOcrRunning = true
                    ocrStatusMessage = "Running offline ML Kit OCR..."
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val result = OcrHelper.recognizeText(bitmap)
                            result.onSuccess { extractedText ->
                                if (extractedText.isNotBlank()) {
                                    rawText = extractedText
                                    ocrStatusMessage = "Text successfully extracted from image!"
                                } else {
                                    ocrStatusMessage = "No text found in selected image."
                                }
                            }.onFailure { err ->
                                ocrStatusMessage = "OCR error: ${err.message}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    ocrStatusMessage = "Failed to load image: ${e.message}"
                } finally {
                    isOcrRunning = false
                }
            }
        }
    }

    val parsedSections = remember(documentTitle, rawText) {
        SectionChunker.chunk(documentTitle = documentTitle, rawText = rawText)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Import Material",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Extract notes offline from pasted text or photos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Paste Text") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Scan Photo / OCR") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Title
        OutlinedTextField(
            value = documentTitle,
            onValueChange = { documentTitle = it },
            label = { Text("Document Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Paste Text Tab
            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                label = { Text("Notes Content") },
                placeholder = { Text("Paste notes here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        rawText = """Philippine History: The Philippine Revolution began in August 1896, upon the discovery of the Katipunan secret society by Spanish colonial authorities. Led by Andres Bonifacio, members tore their cedulas during the Cry of Pugad Lawin.

The declaration of Philippine Independence occurred on June 12, 1898, in Kawit, Cavite, where General Emilio Aguinaldo proclaimed freedom and unfurled the Philippine national flag."""
                        documentTitle = "Philippine History: 1896 Revolution"
                    }
                ) {
                    Text("Load Sample Notes")
                }

                OutlinedButton(onClick = { rawText = "" }) {
                    Text("Clear")
                }
            }
        } else {
            // Scan Photo Tab
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bundled ML Kit Text Recognition (Latin Script)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% offline OCR. Supports English, Filipino, Taglish, and Bisaya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isOcrRunning
                    ) {
                        Text(if (isOcrRunning) "Extracting Text..." else "Pick Photo to Scan")
                    }

                    if (isOcrRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    }

                    ocrStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Language Mode Selector
        Text(text = "Target Study Language:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LanguageMode.entries.forEach { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { selectedLanguage = lang },
                    label = { Text(lang.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Sections Preview (${parsedSections.size} section(s) detected)",
                    style = MaterialTheme.typography.titleSmall
                )
                parsedSections.forEachIndexed { idx, sec ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${idx + 1}. ${sec.title} (${sec.wordCount} words)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Final Generate Button
        Button(
            onClick = { onSectionsReady(parsedSections, selectedLanguage) },
            enabled = parsedSections.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Proceed to Study Hub (${parsedSections.size} Sections)")
        }
    }
}
