package com.devscion.genai_pg_kmp.responseformatter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun FormattedResponseContent(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { FormattedResponseParser.parse(text) }
    val normalizedText = remember(text) { FormattedResponseParser.normalize(text) }

    if (blocks.isEmpty()) {
        Text(
            text = normalizedText,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is FormattedResponseBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(buildInlineAnnotatedString(block.content), style = style)
                }

                is FormattedResponseBlock.Paragraph -> {
                    Text(
                        text = buildInlineAnnotatedString(block.content),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is FormattedResponseBlock.BulletList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("\u2022", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = buildInlineAnnotatedString(item),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                is FormattedResponseBlock.NumberedList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEachIndexed { index, item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = buildInlineAnnotatedString(item),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                is FormattedResponseBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                is FormattedResponseBlock.Table -> ResponseTable(block)
            }
        }
    }
}

@Composable
private fun ResponseTable(table: FormattedResponseBlock.Table) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
    ) {
        ResponseTableRow(
            cells = table.headers,
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = borderColor,
            isHeader = true
        )
        table.rows.forEach { row ->
            ResponseTableRow(
                cells = row,
                background = MaterialTheme.colorScheme.surface,
                borderColor = borderColor,
                isHeader = false
            )
        }
    }
}

@Composable
private fun ResponseTableRow(
    cells: List<String>,
    background: Color,
    borderColor: Color,
    isHeader: Boolean
) {
    Row(modifier = Modifier.background(background)) {
        cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .border(0.5.dp, borderColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = FormattedResponseParser.normalize(cell),
                    style = if (isHeader) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

private fun buildInlineAnnotatedString(
    content: List<FormattedResponseInline>
): AnnotatedString = buildAnnotatedString {
    appendInlineNodes(content)
}

private fun AnnotatedString.Builder.appendInlineNodes(
    nodes: List<FormattedResponseInline>
) {
    nodes.forEach { node ->
        when (node) {
            is FormattedResponseInline.Text -> append(node.value)
            is FormattedResponseInline.Bold -> withStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold)
            ) {
                appendInlineNodes(node.children)
            }

            is FormattedResponseInline.Italic -> withStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic)
            ) {
                appendInlineNodes(node.children)
            }

            is FormattedResponseInline.InlineCode -> withStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0x1A808080)
                )
            ) {
                append(node.value)
            }
        }
    }
}
