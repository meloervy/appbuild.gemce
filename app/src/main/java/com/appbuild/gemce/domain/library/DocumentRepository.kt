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
