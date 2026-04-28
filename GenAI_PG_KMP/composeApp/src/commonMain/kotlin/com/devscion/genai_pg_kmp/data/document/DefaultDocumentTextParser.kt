package com.devscion.genai_pg_kmp.data.document

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.FilePath
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.document.DocumentTextParser
import com.devscion.genai_pg_kmp.domain.document.PdfTextExtractor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DefaultDocumentTextParser(
    private val pathProvider: ModelPathProvider,
    private val pdfTextExtractor: PdfTextExtractor,
) : DocumentTextParser {

    private val logger = Logger.withTag("DocumentTextParser")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun parse(file: PlatformFile): String? {
        val parsed = file.pathOrUri.takeIf { it.isNotBlank() }?.let { parse(it, file.name) }
        if (parsed.isNullOrBlank().not()) return parsed
        return file.content?.let(::normalizePlainText)?.takeIf { it.isNotBlank() }
    }

    override suspend fun parse(path: FilePath, fileName: String?): String? {
        val extension = fileName?.extensionOrNull() ?: path.extensionOrNull()
        return runCatching {
            when (extension) {
                "pdf" -> pdfTextExtractor.extractText(path)?.let(::normalizePlainText)
                "csv" -> readText(path)?.let { formatDelimitedText(it, ',') }
                "tsv" -> readText(path)?.let { formatDelimitedText(it, '\t') }
                "json" -> readText(path)?.let(::formatJsonText)
                "jsonl" -> readText(path)?.let(::formatJsonLinesText)
                "html", "htm" -> readText(path)?.let(::formatHtmlText)
                "xml" -> readText(path)?.let(::formatXmlText)
                "md", "markdown" -> readText(path)?.let(::formatMarkdownText)
                in PLAIN_TEXT_EXTENSIONS -> readText(path)?.let(::normalizePlainText)
                else -> readText(path)
                    ?.takeIf { it.looksLikeText() }
                    ?.let(::normalizePlainText)
            }?.takeIf { it.isNotBlank() }
        }.onFailure { error ->
            logger.e(error) { "Failed to parse document: ${fileName ?: path}" }
        }.getOrNull()
    }

    private suspend fun readText(path: FilePath): String? = pathProvider.getContentText(path)

    private fun formatDelimitedText(raw: String, delimiter: Char): String {
        val rows = parseDelimitedRows(raw, delimiter)
            .map { row -> row.map { cell -> cell.cleanCell() } }
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return normalizePlainText(raw)

        val headerCandidate = rows.first().all { it.isNotBlank() } && rows.size > 1
        val headers = if (headerCandidate) {
            rows.first().mapIndexed { index, value -> value.ifBlank { "Column ${index + 1}" } }
        } else {
            List(rows.maxOf { it.size }) { index -> "Column ${index + 1}" }
        }
        val dataRows = if (headerCandidate) rows.drop(1) else rows

        return buildString {
            append("Columns: ")
            append(headers.joinToString(", "))
            dataRows.forEachIndexed { rowIndex, row ->
                val values = headers.mapIndexedNotNull { index, header ->
                    row.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { "$header: $it" }
                }
                if (values.isNotEmpty()) {
                    append("\n\nRow ")
                    append(rowIndex + 1)
                    append(": ")
                    append(values.joinToString(" | "))
                }
            }
        }.trim()
    }

    private fun parseDelimitedRows(raw: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < raw.length) {
            val char = raw[index]
            when {
                char == '"' -> {
                    if (insideQuotes && raw.getOrNull(index + 1) == '"') {
                        currentCell.append('"')
                        index += 1
                    } else {
                        insideQuotes = insideQuotes.not()
                    }
                }

                char == delimiter && insideQuotes.not() -> {
                    currentRow += currentCell.toString()
                    currentCell.clear()
                }

                (char == '\n' || char == '\r') && insideQuotes.not() -> {
                    currentRow += currentCell.toString()
                    currentCell.clear()
                    rows += currentRow.toList()
                    currentRow.clear()
                    if (char == '\r' && raw.getOrNull(index + 1) == '\n') {
                        index += 1
                    }
                }

                else -> currentCell.append(char)
            }
            index += 1
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += currentCell.toString()
            rows += currentRow.toList()
        }

        return rows
    }

    private fun formatJsonText(raw: String): String {
        val element = runCatching { json.parseToJsonElement(raw) }.getOrElse {
            return normalizePlainText(raw)
        }
        return flattenJsonElement(element).ifBlank { normalizePlainText(raw) }
    }

    private fun formatJsonLinesText(raw: String): String {
        val lines = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                runCatching { json.parseToJsonElement(line) }.getOrNull()?.let(::flattenJsonElement)
                    ?: normalizePlainText(line).takeIf { it.isNotBlank() }
            }
            .toList()

        return if (lines.isEmpty()) normalizePlainText(raw) else lines.joinToString("\n\n")
    }

    private fun flattenJsonElement(element: JsonElement): String {
        val lines = mutableListOf<String>()
        appendJsonLines(path = "", element = element, lines = lines)
        return lines.joinToString("\n").trim()
    }

    private fun appendJsonLines(path: String, element: JsonElement, lines: MutableList<String>) {
        when (element) {
            is JsonObject -> {
                element.entries.forEach { (key, value) ->
                    val childPath = if (path.isBlank()) key else "$path.$key"
                    appendJsonLines(childPath, value, lines)
                }
            }

            is JsonArray -> {
                if (element.all { it is JsonPrimitive }) {
                    val values = element.joinToString(", ") { primitiveText(it as JsonPrimitive) }
                    if (values.isNotBlank()) {
                        lines += "${path.ifBlank { "items" }}: $values"
                    }
                } else {
                    element.forEachIndexed { index, value ->
                        appendJsonLines("${path.ifBlank { "items" }}[$index]", value, lines)
                    }
                }
            }

            is JsonPrimitive -> {
                val value = primitiveText(element)
                if (value.isNotBlank()) {
                    lines += "${path.ifBlank { "value" }}: $value"
                }
            }
        }
    }

    private fun primitiveText(primitive: JsonPrimitive): String =
        primitive
            .takeUnless { it.toString() == "null" }
            ?.content
            ?.trim()
            .orEmpty()

    private fun formatMarkdownText(raw: String): String {
        val text = raw
            .replace(Regex("!\\[([^]]*)]\\(([^)]+)\\)"), "$1")
            .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1 ($2)")
            .replace(Regex("(?m)^#{1,6}\\s*"), "")
            .replace(Regex("(?m)^>\\s?"), "")
            .replace(Regex("(?m)^[-*+]\\s+"), "- ")
            .replace(Regex("(?m)^\\d+\\.\\s+"), "")
            .replace("```", "")
            .replace("`", "")
        return normalizePlainText(text)
    }

    private fun formatHtmlText(raw: String): String {
        val text = raw
            .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?is)<br\\s*/?>"), "\n")
            .replace(Regex("(?is)</(p|div|section|article|header|footer|aside|main|table|tr|ul|ol|li|h[1-6])>"), "\n")
            .replace(Regex("(?is)<li[^>]*>"), "- ")
            .replace(Regex("(?is)<[^>]+>"), " ")
        return normalizeMarkupText(decodeHtmlEntities(text))
    }

    private fun formatXmlText(raw: String): String {
        val text = raw
            .replace(Regex("(?is)<\\?xml[^>]*\\?>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")
            .replace(Regex("(?is)<[^>]+>"), " ")
        return normalizeMarkupText(decodeHtmlEntities(text))
    }

    private fun decodeHtmlEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    private fun normalizePlainText(raw: String): String = raw
        .replace("\u0000", "")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .map { it.trimEnd() }
        .joinToString("\n")
        .replace(Regex("[\\t\\x0B\\f]+"), " ")
        .replace(Regex(" {2,}"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun normalizeMarkupText(raw: String): String = normalizePlainText(raw)
        .lineSequence()
        .map { it.trim() }
        .joinToString("\n")
        .replace(Regex(" +([.,;:!?])"), "$1")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun String.cleanCell(): String = normalizePlainText(this)

    private fun String.looksLikeText(): Boolean {
        if (isBlank()) return true
        val suspiciousCharacters = count { character ->
            (character < ' ' && character !in listOf('\n', '\r', '\t')) || character == '\uFFFD'
        }
        return suspiciousCharacters * 5 < length
    }

    private fun String.extensionOrNull(): String? {
        val cleaned = substringBefore('?').substringBefore('#')
        val extension = cleaned.substringAfterLast('.', "")
            .trim()
            .lowercase()
        return extension.takeIf { it.isNotBlank() }
    }

    private companion object {
        val PLAIN_TEXT_EXTENSIONS = setOf(
            "txt",
            "text",
            "mdx",
            "log",
            "yaml",
            "yml",
            "ini",
            "cfg",
            "conf",
            "properties",
            "sql"
        )
    }
}
