package com.appbuild.gemce.domain.ingest.parsers

import java.io.InputStream

object MarkdownExtractor {

    /**
     * Reads and normalizes Markdown or plain text from an InputStream.
     */
    fun extractText(inputStream: InputStream): String {
        return inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText().replace("\r\n", "\n").trim()
        }
    }
}
