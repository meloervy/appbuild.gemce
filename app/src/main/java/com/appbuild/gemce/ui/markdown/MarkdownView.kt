package com.appbuild.gemce.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val blocks = remember(markdown) { MarkdownParser.parseBlocks(markdown) }

    val codeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val codeTextColor = MaterialTheme.colorScheme.primary
    val citationColor = MaterialTheme.colorScheme.secondary
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        3 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = MarkdownParser.buildInlineAnnotatedString(
                            text = block.text,
                            codeBackground = codeBg,
                            codeTextColor = codeTextColor,
                            citationColor = citationColor,
                            linkColor = linkColor
                        ),
                        style = headingStyle,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = MarkdownParser.buildInlineAnnotatedString(
                            text = block.text,
                            codeBackground = codeBg,
                            codeTextColor = codeTextColor,
                            citationColor = citationColor,
                            linkColor = linkColor
                        ),
                        style = style,
                        color = color
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        language = block.language,
                        code = block.code
                    )
                }

                is MarkdownBlock.Quote -> {
                    QuoteView(
                        text = block.text,
                        codeBg = codeBg,
                        codeTextColor = codeTextColor,
                        citationColor = citationColor,
                        linkColor = linkColor
                    )
                }

                is MarkdownBlock.BulletList -> {
                    BulletListView(
                        items = block.items,
                        style = style,
                        color = color,
                        codeBg = codeBg,
                        codeTextColor = codeTextColor,
                        citationColor = citationColor,
                        linkColor = linkColor
                    )
                }

                is MarkdownBlock.NumberedList -> {
                    NumberedListView(
                        items = block.items,
                        style = style,
                        color = color,
                        codeBg = codeBg,
                        codeTextColor = codeTextColor,
                        citationColor = citationColor,
                        linkColor = linkColor
                    )
                }

                is MarkdownBlock.Table -> {
                    TableView(
                        headers = block.headers,
                        rows = block.rows,
                        codeBg = codeBg,
                        codeTextColor = codeTextColor,
                        citationColor = citationColor,
                        linkColor = linkColor
                    )
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockView(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "CODE" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(code))
                                isCopied = true
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isCopied) "✓ Copied" else "📋 Copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun QuoteView(
    text: String,
    codeBg: Color,
    codeTextColor: Color,
    citationColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 6.dp, horizontal = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = MarkdownParser.buildInlineAnnotatedString(
                    text = text,
                    codeBackground = codeBg,
                    codeTextColor = codeTextColor,
                    citationColor = citationColor,
                    linkColor = linkColor
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BulletListView(
    items: List<String>,
    style: TextStyle,
    color: Color,
    codeBg: Color,
    codeTextColor: Color,
    citationColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { itemText ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = style.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                )
                Text(
                    text = MarkdownParser.buildInlineAnnotatedString(
                        text = itemText,
                        codeBackground = codeBg,
                        codeTextColor = codeTextColor,
                        citationColor = citationColor,
                        linkColor = linkColor
                    ),
                    style = style,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NumberedListView(
    items: List<Pair<Int, String>>,
    style: TextStyle,
    color: Color,
    codeBg: Color,
    codeTextColor: Color,
    citationColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { (number, itemText) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$number.",
                    style = style.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                )
                Text(
                    text = MarkdownParser.buildInlineAnnotatedString(
                        text = itemText,
                        codeBackground = codeBg,
                        codeTextColor = codeTextColor,
                        citationColor = citationColor,
                        linkColor = linkColor
                    ),
                    style = style,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TableView(
    headers: List<String>,
    rows: List<List<String>>,
    codeBg: Color,
    codeTextColor: Color,
    citationColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                headers.forEach { h ->
                    Text(
                        text = MarkdownParser.buildInlineAnnotatedString(
                            text = h,
                            codeBackground = codeBg,
                            codeTextColor = codeTextColor,
                            citationColor = citationColor,
                            linkColor = linkColor
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 6.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Data Rows
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = MarkdownParser.buildInlineAnnotatedString(
                                text = cell,
                                codeBackground = codeBg,
                                codeTextColor = codeTextColor,
                                citationColor = citationColor,
                                linkColor = linkColor
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 6.dp)
                        )
                    }
                }
                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
