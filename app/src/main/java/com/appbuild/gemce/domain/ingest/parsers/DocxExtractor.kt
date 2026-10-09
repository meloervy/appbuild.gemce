package com.appbuild.gemce.domain.ingest.parsers

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocxExtractor {

    /**
     * Extracts text paragraphs from a .docx file stream.
     */
    fun extractText(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        val fullText = StringBuilder()

        try {
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")

                    var eventType = parser.eventType
                    var inParagraph = false
                    val paragraphText = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name) {
                                    "p" -> {
                                        inParagraph = true
                                        paragraphText.clear()
                                    }
                                    "t" -> {
                                        if (inParagraph && parser.next() == XmlPullParser.TEXT) {
                                            paragraphText.append(parser.text)
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "p") {
                                    inParagraph = false
                                    val line = paragraphText.toString().trim()
                                    if (line.isNotEmpty()) {
                                        fullText.append(line).append("\n\n")
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        } finally {
            zip.close()
        }

        return fullText.toString().trim()
    }
}
