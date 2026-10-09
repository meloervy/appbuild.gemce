package com.appbuild.gemce.domain.ingest.parsers

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.TreeMap
import java.util.zip.ZipInputStream

object PptxExtractor {

    /**
     * Extracts text from presentation slides in a .pptx file stream.
     */
    fun extractText(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        val slidesMap = TreeMap<Int, String>()

        try {
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                    val slideNumber = name.removePrefix("ppt/slides/slide")
                        .removeSuffix(".xml")
                        .toIntOrNull() ?: 1

                    val parser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")

                    val slideText = StringBuilder()
                    var eventType = parser.eventType

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                            if (parser.next() == XmlPullParser.TEXT) {
                                slideText.append(parser.text).append(" ")
                            }
                        }
                        eventType = parser.next()
                    }

                    val content = slideText.toString().trim()
                    if (content.isNotEmpty()) {
                        slidesMap[slideNumber] = content
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        } finally {
            zip.close()
        }

        val fullText = StringBuilder()
        for ((num, content) in slidesMap) {
            fullText.append("[Slide $num]\n").append(content).append("\n\n")
        }

        return fullText.toString().trim()
    }
}
