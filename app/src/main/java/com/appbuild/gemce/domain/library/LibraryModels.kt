package com.appbuild.gemce.domain.library

import com.appbuild.gemce.domain.ingest.Section
import java.util.UUID

enum class AcademicFileType(val extensionLabel: String, val icon: String) {
    PDF("PDF", "📄"),
    DOCX("DOCX", "📝"),
    PPTX("PPTX", "📊"),
    MARKDOWN("MD", "📑"),
    TXT("TXT", "📃"),
    PHOTO("PHOTO", "📷"),
    PASTE("NOTE", "📌")
}

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class DocumentItem(
    val id: String = UUID.randomUUID().toString(),
    val folderId: String? = null,
    val title: String,
    val fileType: AcademicFileType,
    val rawText: String,
    val sections: List<Section>,
    val isIncludedInContext: Boolean = true,
    val wordCount: Int = rawText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size,
    val createdAt: Long = System.currentTimeMillis()
)
