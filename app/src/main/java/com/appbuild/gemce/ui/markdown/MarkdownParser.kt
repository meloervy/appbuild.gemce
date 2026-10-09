package com.appbuild.gemce.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

object MarkdownParser {

    private val INLINE_PATTERN = Pattern.compile(
        "(`[^`]+`)" +
        "|(\\*{3}[^*]+\\*{3})" +
        "|(\\*{2}[^*]+\\*{2})" +
        "|(__[^_]+__)" +
        "|(?<=\\s|^|[(\\[])(\\*[^*\\s][^*]*\\*)(?=[)\\],.\\s]|$)" +
        "|(?<=\\s|^|[(\\[])(_[^_\\s][^_]*_)(?=[)\\],.\\s]|$)" +
        "|(~~[^~]+~~)" +
        "|(\\[(?:S\\d+|unverified|General Academic[^\\]]*)\\])" +
        "|(\\[([^\\]]+)\\]\\(([^)]+)\\))"
    )

    private val NUMBERED_LIST_PATTERN = Pattern.compile("^(\\d+)[.)]\\s+(.*)$")
    private val BULLET_LIST_PATTERN = Pattern.compile("^[-*+]\\s+(.*)$")
    private val HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$")
    private val BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s*(.*)$")

    /**
     * Parses a raw markdown string into a structured list of [MarkdownBlock] elements.
     */
    fun parseBlocks(rawMarkdown: String): List<MarkdownBlock> {
        val normalized = rawMarkdown.replace("\r\n", "\n")
        val lines = normalized.lines()
        val blocks = mutableListOf<MarkdownBlock>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Empty lines
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 2. Fenced Code Blocks (```lang ... ```)
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size && lines[i].trim().startsWith("```")) {
                    i++ // consume closing ```
                }
                blocks.add(MarkdownBlock.CodeBlock(language = language, code = codeLines.joinToString("\n")))
                continue
            }

            // 3. Horizontal Dividers (---, ***, ___)
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // 4. Headings (#, ##, ###, ...)
            val headingMatcher = HEADING_PATTERN.matcher(trimmed)
            if (headingMatcher.matches()) {
                val level = headingMatcher.group(1)?.length ?: 1
                val text = headingMatcher.group(2) ?: ""
                blocks.add(MarkdownBlock.Heading(level = level, text = text))
                i++
                continue
            }

            // 5. Blockquotes (> ...)
            if (BLOCKQUOTE_PATTERN.matcher(trimmed).matches()) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size) {
                    val qMatcher = BLOCKQUOTE_PATTERN.matcher(lines[i].trim())
                    if (qMatcher.matches()) {
                        quoteLines.add(qMatcher.group(1) ?: "")
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.Quote(text = quoteLines.joinToString("\n")))
                continue
            }

            // 6. Tables (| Col 1 | Col 2 |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size) {
                val nextTrimmed = lines[i + 1].trim()
                if (nextTrimmed.startsWith("|") && nextTrimmed.contains("---")) {
                    // Valid table header + separator found
                    val headers = parseTableRow(trimmed)
                    i += 2 // skip header and separator
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size) {
                        val rowLine = lines[i].trim()
                        if (rowLine.startsWith("|") && rowLine.endsWith("|")) {
                            rows.add(parseTableRow(rowLine))
                            i++
                        } else {
                            break
                        }
                    }
                    blocks.add(MarkdownBlock.Table(headers = headers, rows = rows))
                    continue
                }
            }

            // 7. Bullet Lists (- , * , + )
            if (BULLET_LIST_PATTERN.matcher(trimmed).matches()) {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val bMatcher = BULLET_LIST_PATTERN.matcher(lines[i].trim())
                    if (bMatcher.matches()) {
                        items.add(bMatcher.group(1) ?: "")
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.BulletList(items = items))
                continue
            }

            // 8. Numbered Lists (1. , 2. )
            if (NUMBERED_LIST_PATTERN.matcher(trimmed).matches()) {
                val items = mutableListOf<Pair<Int, String>>()
                while (i < lines.size) {
                    val nMatcher = NUMBERED_LIST_PATTERN.matcher(lines[i].trim())
                    if (nMatcher.matches()) {
                        val num = nMatcher.group(1)?.toIntOrNull() ?: (items.size + 1)
                        val text = nMatcher.group(2) ?: ""
                        items.add(Pair(num, text))
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.NumberedList(items = items))
                continue
            }

            // 9. Standard Paragraph (accumulate consecutive non-empty lines until next block)
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val pLine = lines[i].trim()
                if (pLine.isEmpty() ||
                    pLine.startsWith("```") ||
                    pLine == "---" || pLine == "***" || pLine == "___" ||
                    HEADING_PATTERN.matcher(pLine).matches() ||
                    BLOCKQUOTE_PATTERN.matcher(pLine).matches() ||
                    BULLET_LIST_PATTERN.matcher(pLine).matches() ||
                    NUMBERED_LIST_PATTERN.matcher(pLine).matches() ||
                    (pLine.startsWith("|") && pLine.endsWith("|"))
                ) {
                    break
                }
                paragraphLines.add(pLine)
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(text = paragraphLines.joinToString("\n")))
            }
        }

        return blocks
    }

    private fun parseTableRow(row: String): List<String> {
        return row.trim()
            .removeSurrounding("|")
            .split("|")
            .map { it.trim() }
    }

    /**
     * Builds an [AnnotatedString] with inline styles for bold, italic, code, strikethrough, and citations.
     */
    fun buildInlineAnnotatedString(
        text: String,
        codeBackground: Color = Color(0x33888888),
        codeTextColor: Color = Color(0xFFE0E0E0),
        citationColor: Color = Color(0xFF4DD0E1),
        linkColor: Color = Color(0xFF64B5F6)
    ): AnnotatedString {
        return buildAnnotatedString {
            val matcher = INLINE_PATTERN.matcher(text)
            var lastIndex = 0

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                // Append unformatted text before the match
                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }

                val matchedString = matcher.group()

                when {
                    // Inline Code: `code`
                    matchedString.startsWith("`") && matchedString.endsWith("`") && matchedString.length >= 2 -> {
                        val content = matchedString.substring(1, matchedString.length - 1)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBackground,
                                color = codeTextColor
                            )
                        )
                        append(" $content ")
                        pop()
                    }

                    // Bold + Italic: ***text***
                    matchedString.startsWith("***") && matchedString.endsWith("***") && matchedString.length >= 6 -> {
                        val content = matchedString.substring(3, matchedString.length - 3)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                        append(content)
                        pop()
                    }

                    // Bold: **text**
                    matchedString.startsWith("**") && matchedString.endsWith("**") && matchedString.length >= 4 -> {
                        val content = matchedString.substring(2, matchedString.length - 2)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(content)
                        pop()
                    }

                    // Bold: __text__
                    matchedString.startsWith("__") && matchedString.endsWith("__") && matchedString.length >= 4 -> {
                        val content = matchedString.substring(2, matchedString.length - 2)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(content)
                        pop()
                    }

                    // Italic: *text*
                    matchedString.startsWith("*") && matchedString.endsWith("*") && matchedString.length >= 2 -> {
                        val content = matchedString.substring(1, matchedString.length - 1)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(content)
                        pop()
                    }

                    // Italic: _text_
                    matchedString.startsWith("_") && matchedString.endsWith("_") && matchedString.length >= 2 -> {
                        val content = matchedString.substring(1, matchedString.length - 1)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(content)
                        pop()
                    }

                    // Strikethrough: ~~text~~
                    matchedString.startsWith("~~") && matchedString.endsWith("~~") && matchedString.length >= 4 -> {
                        val content = matchedString.substring(2, matchedString.length - 2)
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(content)
                        pop()
                    }

                    // Citation / Note Badge: [S1], [unverified], [General Academic ...]
                    matchedString.startsWith("[") && matchedString.endsWith("]") && !matchedString.contains("(") -> {
                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = citationColor
                            )
                        )
                        append(matchedString)
                        pop()
                    }

                    // Markdown Link: [text](url)
                    matchedString.startsWith("[") && matchedString.contains("](") && matchedString.endsWith(")") -> {
                        val linkText = matcher.group(10) ?: matchedString
                        pushStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(linkText)
                        pop()
                    }

                    else -> {
                        append(matchedString)
                    }
                }

                lastIndex = end
            }

            // Append any remaining text
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
