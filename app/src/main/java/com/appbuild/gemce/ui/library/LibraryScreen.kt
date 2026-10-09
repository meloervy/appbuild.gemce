package com.appbuild.gemce.ui.library

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.domain.ingest.OcrHelper
import com.appbuild.gemce.domain.ingest.SectionChunker
import com.appbuild.gemce.domain.ingest.parsers.DocxExtractor
import com.appbuild.gemce.domain.ingest.parsers.MarkdownExtractor
import com.appbuild.gemce.domain.ingest.parsers.PdfExtractor
import com.appbuild.gemce.domain.ingest.parsers.PptxExtractor
import com.appbuild.gemce.domain.library.AcademicFileType
import com.appbuild.gemce.domain.library.DocumentItem
import com.appbuild.gemce.domain.library.DocumentRepository
import com.appbuild.gemce.domain.library.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onStudyDocument: (document: DocumentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by DocumentRepository.folders.collectAsState()
    val documents by DocumentRepository.documents.collectAsState()

    var selectedFolderId by remember { mutableStateOf<String?>(null) } // null = All
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showAddMaterialSheet by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<DocumentItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredDocs = documents
        .filter { selectedFolderId == null || it.folderId == selectedFolderId }
        .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.rawText.contains(searchQuery, ignoreCase = true) }

    val activeContextDocsCount = documents.count { it.isIncludedInContext }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddMaterialSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Text("➕", fontWeight = FontWeight.Bold) },
                text = { Text("Add Material", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Academic Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$activeContextDocsCount of ${documents.size} file(s) active in AI context",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showNewFolderDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Folder", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes, curriculum, or topics...") },
                leadingIcon = { Text("🔍") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Folder Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                        label = { Text("All (${documents.size})") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(folders) { folder ->
                    val count = documents.count { it.folderId == folder.id }
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = {
                            selectedFolderId = if (selectedFolderId == folder.id) null else folder.id
                        },
                        label = { Text("📁 ${folder.name} ($count)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Documents List or Empty State
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "📚",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (documents.isEmpty()) "Your Library is Empty" else "No matching documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (documents.isEmpty()) {
                                "Tap '+ Add Material' below to import course notes, syllabi, PDF slides, or photos."
                            } else {
                                "Try searching for a different term or select another folder."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (documents.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddMaterialSheet = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("➕ Add Your First Note")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        val folderName = folders.find { it.id == doc.folderId }?.name
                        DocumentCard(
                            doc = doc,
                            folderName = folderName,
                            onToggleContext = { DocumentRepository.toggleContextInclusion(doc.id) },
                            onViewNotes = { viewingDocument = doc },
                            onStudy = { onStudyDocument(doc) },
                            onDelete = { DocumentRepository.deleteDocument(doc.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Clearance for FAB
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Unified Import / Add Material
    if (showAddMaterialSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddMaterialSheet = false },
            sheetState = sheetState
        ) {
            AddMaterialSheetContent(
                folders = folders,
                onDismiss = { showAddMaterialSheet = false },
                onDocumentCreated = { createdDoc, studyNow ->
                    DocumentRepository.addDocument(createdDoc)
                    showAddMaterialSheet = false
                    if (studyNow) {
                        onStudyDocument(createdDoc)
                    }
                },
                onQuickCreateFolder = { folderName ->
                    DocumentRepository.addFolder(folderName)
                }
            )
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. System Design, Calculus, Organic Chemistry...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            DocumentRepository.addFolder(newFolderName.trim())
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Notes Dialog
    viewingDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = { viewingDocument = null },
            title = { Text(doc.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${doc.fileType.icon} ${doc.fileType.extensionLabel} • ${doc.wordCount} words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${doc.sections.size} section(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = doc.rawText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewingDocument = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DocumentCard(
    doc: DocumentItem,
    folderName: String?,
    onToggleContext: () -> Unit,
    onViewNotes: () -> Unit,
    onStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: File Type Pill, Title, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${doc.fileType.icon} ${doc.fileType.extensionLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.clickable { onDelete() }
                ) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle & Folder Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${doc.wordCount} words • ${doc.sections.size} section(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (folderName != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "📁 $folderName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Context Toggle
            Surface(
                color = if (doc.isIncludedInContext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (doc.isIncludedInContext) "🤖 In AI Context (Gemma 4 E2B)" else "Disabled for AI Context",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (doc.isIncludedInContext) FontWeight.Bold else FontWeight.Normal,
                            color = if (doc.isIncludedInContext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = doc.isIncludedInContext,
                        onCheckedChange = { onToggleContext() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Read Notes & Study Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewNotes,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Read Notes", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onStudy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Study Now 🚀", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddMaterialSheetContent(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onDocumentCreated: (doc: DocumentItem, studyNow: Boolean) -> Unit,
    onQuickCreateFolder: (folderName: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Files, 1: Scan / OCR, 2: Paste Text
    var documentTitle by remember { mutableStateOf("") }
    var detectedFileType by remember { mutableStateOf(AcademicFileType.PASTE) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var rawText by remember { mutableStateOf("") }
    var includeInContext by remember { mutableStateOf(true) }

    var isProcessingFile by remember { mutableStateOf(false) }
    var extractionStatusMessage by remember { mutableStateOf<String?>(null) }
    var showInlineFolderCreator by remember { mutableStateOf(false) }
    var inlineFolderName by remember { mutableStateOf("") }

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

    // Document File Picker (PDF, DOCX, PPTX, MD, TXT)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingFile = true
                val fileName = getFileName(uri)
                documentTitle = fileName
                extractionStatusMessage = "Parsing $fileName locally..."

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
                                    extractionStatusMessage = "✓ Extracted ${text.split("\\s+".toRegex()).size} words successfully!"
                                } else {
                                    extractionStatusMessage = "⚠️ File appears empty or could not be parsed."
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            extractionStatusMessage = "Failed: ${e.message}"
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
                                    if (documentTitle.isBlank()) {
                                        documentTitle = "Scanned Note ${System.currentTimeMillis() % 10000}"
                                    }
                                    extractionStatusMessage = "✓ OCR extracted ${extracted.split("\\s+".toRegex()).size} words!"
                                } else {
                                    extractionStatusMessage = "No text found in photo."
                                }
                            }.onFailure { err ->
                                extractionStatusMessage = "OCR error: ${err.message}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    extractionStatusMessage = "Error: ${e.message}"
                } finally {
                    isProcessingFile = false
                }
            }
        }
    }

    val parsedSections = remember(documentTitle, rawText) {
        SectionChunker.chunk(documentTitle = documentTitle.ifBlank { "Untitled Note" }, rawText = rawText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Modal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📥 Add Learning Material",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Parses 100% locally. Never uploaded to the cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Import Source Tabs
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    detectedFileType = AcademicFileType.PDF
                },
                text = { Text("📄 Files") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    detectedFileType = AcademicFileType.PHOTO
                },
                text = { Text("📸 Scan OCR") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    detectedFileType = AcademicFileType.PASTE
                },
                text = { Text("✏️ Paste") }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Files
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Supports PDF, DOCX, PPTX & Markdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Extracts headings, paragraphs, and sections offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { docPickerLauncher.launch("*/*") },
                            enabled = !isProcessingFile,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📁 Select Document From Storage")
                        }
                    }
                }
            }

            1 -> {
                // OCR
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Offline ML Kit OCR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Photograph a printed book page, handout, or whiteboard.",
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
                            enabled = !isProcessingFile,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 Scan / Select Photo")
                        }
                    }
                }
            }

            2 -> {
                // Paste
                Text(
                    text = "Paste text directly from your lecture notes, website, or syllabus:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress Message
        if (isProcessingFile) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = extractionStatusMessage ?: "Processing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (extractionStatusMessage != null) {
            Text(
                text = extractionStatusMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Title Input
        OutlinedTextField(
            value = documentTitle,
            onValueChange = { documentTitle = it },
            label = { Text("Document / Material Title") },
            placeholder = { Text("e.g. CS 301 - Microservices Architecture Syllabus") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        // Destination Folder Selector
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Assign to Folder:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (showInlineFolderCreator) "Cancel" else "+ New Folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showInlineFolderCreator = !showInlineFolderCreator }
                )
            }

            if (showInlineFolderCreator) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inlineFolderName,
                        onValueChange = { inlineFolderName = it },
                        placeholder = { Text("e.g. Distributed Systems") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (inlineFolderName.isNotBlank()) {
                                onQuickCreateFolder(inlineFolderName.trim())
                                inlineFolderName = ""
                                showInlineFolderCreator = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    label = { Text("General (No Folder)") },
                    shape = RoundedCornerShape(10.dp)
                )
                folders.forEach { f ->
                    FilterChip(
                        selected = selectedFolderId == f.id,
                        onClick = {
                            selectedFolderId = if (selectedFolderId == f.id) null else f.id
                        },
                        label = { Text("📁 ${f.name}") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Include in AI Context Switch
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Include in AI Context",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Gemma 4 E2B will reference this material during Chat & Tutoring",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = includeInContext,
                    onCheckedChange = { includeInContext = it }
                )
            }
        }

        // Extracted Plain Text Field
        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            label = { Text("Extracted Plain Text (Editable)") },
            placeholder = { Text("Extracted or pasted plain text appears here. You can review and edit before saving.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(14.dp)
        )

        // Section Chunking Preview
        if (parsedSections.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Detected Sections: ${parsedSections.size} chunk(s)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    parsedSections.take(3).forEachIndexed { i, s ->
                        Text(
                            text = "• Section ${i + 1}: ${s.title} (${s.wordCount} words)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (parsedSections.size > 3) {
                        Text(
                            text = "... and ${parsedSections.size - 3} more sections",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Bottom Action Buttons
        val canSave = rawText.isNotBlank() && documentTitle.isNotBlank()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val newDoc = DocumentItem(
                        folderId = selectedFolderId,
                        title = documentTitle.trim(),
                        fileType = detectedFileType,
                        rawText = rawText.trim(),
                        sections = parsedSections,
                        isIncludedInContext = includeInContext
                    )
                    onDocumentCreated(newDoc, false)
                },
                enabled = canSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save to Library", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    val newDoc = DocumentItem(
                        folderId = selectedFolderId,
                        title = documentTitle.trim(),
                        fileType = detectedFileType,
                        rawText = rawText.trim(),
                        sections = parsedSections,
                        isIncludedInContext = includeInContext
                    )
                    onDocumentCreated(newDoc, true)
                },
                enabled = canSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save & Study 🚀", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
