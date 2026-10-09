package com.appbuild.gemce.ui.ingest

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.runtime.collectAsState
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
import com.appbuild.gemce.domain.ingest.parsers.DocxExtractor
import com.appbuild.gemce.domain.ingest.parsers.MarkdownExtractor
import com.appbuild.gemce.domain.ingest.parsers.PdfExtractor
import com.appbuild.gemce.domain.ingest.parsers.PptxExtractor
import com.appbuild.gemce.domain.library.AcademicFileType
import com.appbuild.gemce.domain.library.DocumentItem
import com.appbuild.gemce.domain.library.DocumentRepository
import com.appbuild.gemce.domain.model.LanguageMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IngestScreen(
    onSectionsReady: (sections: List<Section>, language: LanguageMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val folders by DocumentRepository.folders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Files (PDF/DOCX/PPTX/MD), 1: Paste, 2: Photo OCR
    var documentTitle by remember { mutableStateOf("Cell Biology & Energy") }
    var detectedFileType by remember { mutableStateOf(AcademicFileType.PASTE) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }

    var rawText by remember {
        mutableStateOf(
            """Mitochondria are membrane-bound cell organelles that generate most of the chemical energy needed to power the cell's biochemical reactions. Chemical energy produced by the mitochondria is stored in a small molecule called adenosine triphosphate (ATP). 

Mitochondria produce ATP through cellular respiration, converting glucose and oxygen into energy, carbon dioxide, and water. Because ATP is used by cells as universal energy currency, mitochondria are commonly referred to as the powerhouses of the cell."""
        )
    }

    var selectedLanguage by remember { mutableStateOf(LanguageMode.EN) }
    var isProcessingFile by remember { mutableStateOf(false) }
    var extractionStatusMessage by remember { mutableStateOf<String?>(null) }

    fun getFileName(uri: Uri): String {
        var name = "Academic Notes"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    // Document File Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingFile = true
                val fileName = getFileName(uri)
                documentTitle = fileName
                extractionStatusMessage = "Extracting plain text from $fileName..."

                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val text = when {
                                fileName.endsWith(".docx", ignoreCase = true) -> {
                                    detectedFileType = AcademicFileType.DOCX
                                    DocxExtractor.extractText(stream)
                                }
                                fileName.endsWith(".pptx", ignoreCase = true) -> {
                                    detectedFileType = AcademicFileType.PPTX
                                    PptxExtractor.extractText(stream)
                                }
                                fileName.endsWith(".pdf", ignoreCase = true) -> {
                                    detectedFileType = AcademicFileType.PDF
                                    PdfExtractor.extractText(context, stream)
                                }
                                fileName.endsWith(".md", ignoreCase = true) -> {
                                    detectedFileType = AcademicFileType.MARKDOWN
                                    MarkdownExtractor.extractText(stream)
                                }
                                else -> {
                                    detectedFileType = AcademicFileType.TXT
                                    stream.bufferedReader(Charsets.UTF_8).readText()
                                }
                            }

                            withContext(Dispatchers.Main) {
                                if (text.isNotBlank()) {
                                    rawText = text
                                    extractionStatusMessage = "Extracted ${text.split("\\s+".toRegex()).size} words successfully!"
                                } else {
                                    extractionStatusMessage = "File appears empty or could not be parsed."
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            extractionStatusMessage = "Failed to extract: ${e.message}"
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isProcessingFile = false
                        }
                    }
                }
            }
        }
    }

    // Photo OCR Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isProcessingFile = true
                    extractionStatusMessage = "Running offline ML Kit OCR on image..."
                    detectedFileType = AcademicFileType.PHOTO
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val result = OcrHelper.recognizeText(bitmap)
                            result.onSuccess { extracted ->
                                if (extracted.isNotBlank()) {
                                    rawText = extracted
                                    documentTitle = "Scanned Note (${System.currentTimeMillis() % 10000})"
                                    extractionStatusMessage = "Text extracted via offline OCR!"
                                } else {
                                    extractionStatusMessage = "No text found in photo."
                                }
                            }.onFailure { err ->
                                extractionStatusMessage = "OCR error: ${err.message}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    extractionStatusMessage = "Error opening photo: ${e.message}"
                } finally {
                    isProcessingFile = false
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
            text = "Import Academic Notes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Supports PDF, DOCX, PPTX, Markdown, Photos & Text (100% Offline)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Files") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Paste Text") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Photo OCR") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // Academic Files Tab
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Select Academic Document",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PDF, Word (.docx), PowerPoint (.pptx), Markdown (.md), Text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { docPickerLauncher.launch("*/*") },
                            enabled = !isProcessingFile
                        ) {
                            Text(if (isProcessingFile) "Extracting Text..." else "Browse & Open Document")
                        }

                        if (isProcessingFile) {
                            Spacer(modifier = Modifier.height(10.dp))
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        }

                        extractionStatusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = msg, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            1 -> {
                // Paste Text Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            rawText = """Philippine History: The Philippine Revolution began in August 1896, upon the discovery of the Katipunan secret society by Spanish authorities. Led by Andres Bonifacio, members tore their cedulas during the Cry of Pugad Lawin.

On June 12, 1898, Philippine Independence was proclaimed in Kawit, Cavite by General Emilio Aguinaldo, where the national flag was first unfurled and the national anthem played."""
                            documentTitle = "Philippine History: 1896 Revolution"
                            detectedFileType = AcademicFileType.PASTE
                        }
                    ) {
                        Text("Sample Notes")
                    }

                    OutlinedButton(onClick = { rawText = "" }) {
                        Text("Clear")
                    }
                }
            }

            2 -> {
                // Photo OCR Tab
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Offline ML Kit OCR (Latin Script)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Extract text from photos of books, blackboards, and notes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isProcessingFile
                        ) {
                            Text("Pick Photo to OCR")
                        }

                        if (isProcessingFile) {
                            Spacer(modifier = Modifier.height(10.dp))
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        }

                        extractionStatusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = msg, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Title & Folder
        OutlinedTextField(
            value = documentTitle,
            onValueChange = { documentTitle = it },
            label = { Text("Document Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Folder Assignment
        Text(text = "Assign to Folder:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { selectedFolderId = null },
                label = { Text("No Folder") }
            )
            folders.forEach { f ->
                FilterChip(
                    selected = selectedFolderId == f.id,
                    onClick = { selectedFolderId = f.id },
                    label = { Text("📁 ${f.name}") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Raw Text Preview & Edit
        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            label = { Text("Extracted Plain Text (Editable)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${idx + 1}. ${sec.title} (${sec.wordCount} words)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save to Library & Study Button
        Button(
            onClick = {
                // Save document to Library repository
                val newDoc = DocumentItem(
                    folderId = selectedFolderId,
                    title = documentTitle,
                    fileType = detectedFileType,
                    rawText = rawText,
                    sections = parsedSections,
                    isIncludedInContext = true
                )
                DocumentRepository.addDocument(newDoc)

                onSectionsReady(parsedSections, selectedLanguage)
            },
            enabled = parsedSections.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save to Library & Study (${parsedSections.size} Sections)")
        }
    }
}
