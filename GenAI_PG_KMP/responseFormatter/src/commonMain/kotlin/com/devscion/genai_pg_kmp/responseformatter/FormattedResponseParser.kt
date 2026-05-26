package com.devscion.genai_pg_kmp.responseformatter

object FormattedResponseParser {

    fun normalize(raw: String): String {
        if (raw.isEmpty()) return raw

        return raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
    }

    fun parse(raw: String): List<FormattedResponseBlock> {
        val normalized = normalize(raw).trim()
        if (normalized.isEmpty()) return emptyList()

        val lines = normalized.split('\n')
        val blocks = mutableListOf<FormattedResponseBlock>()
        var index = 0

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                index++
                continue
            }

            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim().ifEmpty { null }
                index++
                val codeLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trim() != "```") {
                    codeLines += lines[index]
                    index++
                }
                if (index < lines.size && lines[index].trim() == "```") {
                    index++
                }
                blocks += FormattedResponseBlock.CodeBlock(
                    language = language,
                    code = codeLines.joinToString("\n").trimEnd()
                )
                continue
            }

            parseHeading(trimmed)?.let { heading ->
                blocks += heading
                index++
                continue
            }

            if (isTableHeader(lines, index)) {
                val tableResult = parseTable(lines, index)
                blocks += tableResult.block
                index = tableResult.nextIndex
                continue
            }

            if (isBulletLine(trimmed)) {
                val items = mutableListOf<List<FormattedResponseInline>>()
                while (index < lines.size && isBulletLine(lines[index].trim())) {
                    val itemText = lines[index].trim().drop(2).trim()
                    items += parseInline(itemText)
                    index++
                }
                blocks += FormattedResponseBlock.BulletList(items)
                continue
            }

            if (isNumberedLine(trimmed)) {
                val items = mutableListOf<List<FormattedResponseInline>>()
                while (index < lines.size && isNumberedLine(lines[index].trim())) {
                    val itemText = lines[index].trim().substringAfter('.').trim()
                    items += parseInline(itemText)
                    index++
                }
                blocks += FormattedResponseBlock.NumberedList(items)
                continue
            }

            val paragraphLines = mutableListOf<String>()
            while (index < lines.size) {
                val current = lines[index].trim()
                if (current.isEmpty()) break
                if (current.startsWith("```")) break
                if (parseHeading(current) != null) break
                if (isBulletLine(current)) break
                if (isNumberedLine(current)) break
                if (isTableHeader(lines, index)) break

                paragraphLines += current
                index++
            }
            blocks += FormattedResponseBlock.Paragraph(
                content = parseInline(paragraphLines.joinToString("\n"))
            )
        }

        return blocks
    }

    private fun parseHeading(line: String): FormattedResponseBlock.Heading? {
        val hashes = line.takeWhile { it == '#' }
        if (hashes.isEmpty() || hashes.length > 6) return null
        if (line.length <= hashes.length || line[hashes.length] != ' ') return null

        return FormattedResponseBlock.Heading(
            level = hashes.length,
            content = parseInline(line.drop(hashes.length + 1).trim())
        )
    }

    private fun parseInline(text: String): List<FormattedResponseInline> {
        if (text.isEmpty()) return emptyList()

        val nodes = mutableListOf<FormattedResponseInline>()
        var cursor = 0

        fun appendText(value: String) {
            if (value.isEmpty()) return
            val last = nodes.lastOrNull()
            if (last is FormattedResponseInline.Text) {
                nodes[nodes.lastIndex] = last.copy(value = last.value + value)
            } else {
                nodes += FormattedResponseInline.Text(value)
            }
        }

        while (cursor < text.length) {
            when {
                text.startsWith("**", cursor) -> {
                    val end = text.indexOf("**", startIndex = cursor + 2)
                    if (end != -1) {
                        val content = text.substring(cursor + 2, end)
                        nodes += FormattedResponseInline.Bold(parseInline(content))
                        cursor = end + 2
                    } else {
                        appendText(text.substring(cursor, cursor + 2))
                        cursor += 2
                    }
                }

                text[cursor] == '*' -> {
                    val end = text.indexOf('*', startIndex = cursor + 1)
                    if (end != -1) {
                        val content = text.substring(cursor + 1, end)
                        nodes += FormattedResponseInline.Italic(parseInline(content))
                        cursor = end + 1
                    } else {
                        appendText(text[cursor].toString())
                        cursor++
                    }
                }

                text[cursor] == '`' -> {
                    val end = text.indexOf('`', startIndex = cursor + 1)
                    if (end != -1) {
                        nodes += FormattedResponseInline.InlineCode(
                            text.substring(cursor + 1, end)
                        )
                        cursor = end + 1
                    } else {
                        appendText(text[cursor].toString())
                        cursor++
                    }
                }

                else -> {
                    val nextSpecial = findNextSpecialTokenStart(text, cursor)
                    appendText(text.substring(cursor, nextSpecial))
                    cursor = nextSpecial
                }
            }
        }

        return nodes
    }

    private fun isBulletLine(line: String): Boolean {
        return (line.startsWith("- ") || line.startsWith("* ")) && line.length > 2
    }

    private fun isNumberedLine(line: String): Boolean {
        val prefix = line.takeWhile { it.isDigit() }
        return prefix.isNotEmpty() &&
            line.length > prefix.length + 1 &&
            line[prefix.length] == '.' &&
            line[prefix.length + 1] == ' '
    }

    private fun isTableHeader(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size) return false
        val header = lines[index].trim()
        val separator = lines[index + 1].trim()
        return header.contains('|') &&
            separator.contains('|') &&
            parseTableCells(separator).all { cell ->
                cell.isNotEmpty() && cell.all { it == '-' || it == ':' }
            }
    }

    private data class TableParseResult(
        val block: FormattedResponseBlock.Table,
        val nextIndex: Int
    )

    private fun parseTable(lines: List<String>, startIndex: Int): TableParseResult {
        val headers = parseTableCells(lines[startIndex].trim())
        val rows = mutableListOf<List<String>>()
        var index = startIndex + 2

        while (index < lines.size) {
            val line = lines[index].trim()
            if (line.isEmpty() || !line.contains('|')) break
            val row = parseTableCells(line)
            rows += headers.indices.map { columnIndex -> row.getOrElse(columnIndex) { "" } }
            index++
        }

        return TableParseResult(
            block = FormattedResponseBlock.Table(headers = headers, rows = rows),
            nextIndex = index
        )
    }

    private fun parseTableCells(line: String): List<String> {
        return line
            .removePrefix("|")
            .removeSuffix("|")
            .split('|')
            .map { it.trim() }
    }

    private fun findNextSpecialTokenStart(text: String, cursor: Int): Int {
        var nextSpecial = text.length
        val boldIndex = text.indexOf("**", cursor)
        if (boldIndex >= 0 && boldIndex < nextSpecial) {
            nextSpecial = boldIndex
        }
        val italicIndex = text.indexOf('*', cursor)
        if (italicIndex >= 0 && italicIndex < nextSpecial) {
            nextSpecial = italicIndex
        }
        val codeIndex = text.indexOf('`', cursor)
        if (codeIndex >= 0 && codeIndex < nextSpecial) {
            nextSpecial = codeIndex
        }
        return nextSpecial
    }
}
