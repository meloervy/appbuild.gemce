package com.appbuild.gemce.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appbuild.gemce.domain.library.DocumentItem
import com.appbuild.gemce.domain.library.DocumentRepository

@Composable
fun LibraryScreen(
    onStudyDocument: (document: DocumentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by DocumentRepository.folders.collectAsState()
    val documents by DocumentRepository.documents.collectAsState()

    var selectedFolderId by remember { mutableStateOf<String?>(null) } // null = All
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<DocumentItem?>(null) }

    val filteredDocs = if (selectedFolderId == null) {
        documents
    } else {
        documents.filter { it.folderId == selectedFolderId }
    }

    val activeContextDocsCount = documents.count { it.isIncludedInContext }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library & Notes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$activeContextDocsCount document(s) included in AI context",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = { showNewFolderDialog = true }) {
                Text("+ Folder")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Folder Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    label = { Text("All (${documents.size})") }
                )
            }
            items(folders) { folder ->
                val count = documents.count { it.folderId == folder.id }
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = { selectedFolderId = folder.id },
                    label = { Text("📁 ${folder.name} ($count)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Documents List
        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No documents found in this folder.\nImport notes from the Import tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredDocs) { doc ->
                    DocumentCard(
                        doc = doc,
                        onToggleContext = { DocumentRepository.toggleContextInclusion(doc.id) },
                        onViewNotes = { viewingDocument = doc },
                        onStudy = { onStudyDocument(doc) },
                        onDelete = { DocumentRepository.deleteDocument(doc.id) }
                    )
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. Organic Chemistry, Finals...") }
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
            title = { Text(doc.title) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Extracted Plain Text (${doc.wordCount} words, ${doc.sections.size} sections)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = doc.rawText,
                        style = MaterialTheme.typography.bodySmall
                    )
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
    onToggleContext: () -> Unit,
    onViewNotes: () -> Unit,
    onStudy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "${doc.fileType.icon} ${doc.fileType.extensionLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${doc.wordCount} words • ${doc.sections.size} section(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Context Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (doc.isIncludedInContext) "✓ Included in AI Context" else "Excluded from AI Context",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (doc.isIncludedInContext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )

                Switch(
                    checked = doc.isIncludedInContext,
                    onCheckedChange = { onToggleContext() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewNotes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Read Notes", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onStudy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Study Now", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
