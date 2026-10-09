package com.appbuild.gemce.domain.ingest.parsers

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

object PdfExtractor {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            isInitialized = true
        }
    }

    /**
     * Extracts text from a PDF input stream.
     */
    fun extractText(context: Context, inputStream: InputStream): String {
        init(context)
        return try {
            val document = PDDocument.load(inputStream)
            try {
                val stripper = PDFTextStripper()
                stripper.getText(document).trim()
            } finally {
                document.close()
            }
        } catch (e: Exception) {
            "Failed to extract PDF text: ${e.message}"
        }
    }
}
