package com.appbuild.gemce.domain.library

import com.appbuild.gemce.domain.ingest.Section
import com.appbuild.gemce.domain.ingest.SectionChunker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DocumentRepository {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _documents = MutableStateFlow<List<DocumentItem>>(emptyList())
    val documents: StateFlow<List<DocumentItem>> = _documents.asStateFlow()

    init {
        // Pre-populate with default folders and sample academic notes
        val bioFolder = Folder(id = "folder_bio", name = "Cell Biology")
        val histFolder = Folder(id = "folder_hist", name = "Philippine History")
        _folders.value = listOf(bioFolder, histFolder)

        val bioText = """Mitochondria are membrane-bound cell organelles that generate most of the chemical energy needed to power the cell's biochemical reactions. Chemical energy produced by the mitochondria is stored in a small molecule called adenosine triphosphate (ATP).

Mitochondria produce ATP through cellular respiration, converting glucose and oxygen into energy, carbon dioxide, and water. Because ATP is used by cells as universal energy currency, mitochondria are commonly referred to as the powerhouses of the cell."""

        val histText = """The Philippine Revolution began in August 1896, upon the discovery of the Katipunan secret society by Spanish authorities. Led by Andres Bonifacio, members tore their tax certificates (cedulas) during the Cry of Pugad Lawin.

On June 12, 1898, Philippine Independence was proclaimed in Kawit, Cavite by General Emilio Aguinaldo, where the national flag was first unfurled and the national anthem played."""

        val bioDoc = DocumentItem(
            id = "doc_bio_1",
            folderId = bioFolder.id,
            title = "Mitochondria & Cellular Energy.docx",
            fileType = AcademicFileType.DOCX,
            rawText = bioText,
            sections = SectionChunker.chunk("Mitochondria & Cellular Energy", bioText, "Page 1"),
            isIncludedInContext = true
        )

        val histDoc = DocumentItem(
            id = "doc_hist_1",
            folderId = histFolder.id,
            title = "1896 Philippine Revolution Summary.pdf",
            fileType = AcademicFileType.PDF,
            rawText = histText,
            sections = SectionChunker.chunk("1896 Philippine Revolution", histText, "Page 1-2"),
            isIncludedInContext = true
        )

        _documents.value = listOf(bioDoc, histDoc)
    }

    fun addFolder(name: String): Folder {
        val folder = Folder(name = name)
        _folders.update { it + folder }
        return folder
    }

    fun deleteFolder(folderId: String) {
        _folders.update { it.filterNot { f -> f.id == folderId } }
        // Ungroup docs in that folder
        _documents.update { docs ->
            docs.map { if (it.folderId == folderId) it.copy(folderId = null) else it }
        }
    }

    fun addDocument(document: DocumentItem) {
        _documents.update { it + document }
    }

    fun deleteDocument(documentId: String) {
        _documents.update { it.filterNot { d -> d.id == documentId } }
    }

    fun toggleContextInclusion(documentId: String) {
        _documents.update { docs ->
            docs.map {
                if (it.id == documentId) it.copy(isIncludedInContext = !it.isIncludedInContext) else it
            }
        }
    }

    fun getActiveContextDocuments(): List<DocumentItem> {
        return _documents.value.filter { it.isIncludedInContext }
    }

    fun getActiveContextSections(): List<Section> {
        return _documents.value.filter { it.isIncludedInContext }.flatMap { it.sections }
    }
}
