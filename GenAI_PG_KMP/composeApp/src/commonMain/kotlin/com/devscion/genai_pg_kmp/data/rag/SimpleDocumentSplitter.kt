package com.devscion.genai_pg_kmp.data.rag

/**
 * Simple document splitter that breaks text into chunks.
 * Splits by sentences and paragraphs with configurable overlap.
 */
class SimpleDocumentSplitter {
    private val separators: List<String> = listOf("\n\n", "\n", ". ", "! ", "? ", "; ", ", ", " ")


    /**
     * Split text into chunks of approximately chunkSize characters with overlap.
     *
     * @param text The text to split
     * @param chunkSize Target size for each chunk in characters (default: 500)
     * @param overlap Number of characters to overlap between chunks (default: 50)
     * @return List of text chunks
     */
    fun split(
        text: String,
        chunkSize: Int = 500,
        overlap: Int = 50
    ): List<String> {
        if (text.isEmpty()) return emptyList()

        // Split by sentences first (basic splitting on period, exclamation, question mark)
        val sentences = text.split(Regex("[.!?]+\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (sentences.isEmpty()) return listOf(text)

        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            // If adding this sentence would exceed chunk size and current chunk is not empty
            if (currentChunk.length + sentence.length > chunkSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())

                // Start new chunk with overlap from previous chunk
                val overlapText = currentChunk.takeLast(overlap).toString()
                currentChunk = StringBuilder(overlapText)
            }

            if (currentChunk.isNotEmpty()) {
                currentChunk.append(" ")
            }
            currentChunk.append(sentence)
        }

        // Add the last chunk if it has content
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.ifEmpty { listOf(text) }
    }


    fun splitOnCharacter(
        text: String,
        chunkSize: Int = 500,
        overlap: Int = 50,
    ): List<String> {
        if (text.isBlank()) return emptyList()

        require(chunkSize > 0) { "chunkSize must be greater than 0" }

        val normalizedText = text.trim()
        val safeOverlap = overlap.coerceIn(0, chunkSize - 1)
        val minimumStep = (chunkSize - safeOverlap).coerceAtLeast(1)
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < normalizedText.length) {
            val requestedEnd = minOf(start + chunkSize, normalizedText.length)
            val breakPoint = if (requestedEnd >= normalizedText.length) {
                normalizedText.length
            } else {
                findBreakPoint(normalizedText, start, requestedEnd)
                    .coerceIn(start + 1, requestedEnd)
            }

            val chunkText = normalizedText.substring(start, breakPoint).trim()
            if (chunkText.isNotEmpty() && chunks.lastOrNull() != chunkText) {
                chunks.add(chunkText)
            }

            if (breakPoint >= normalizedText.length) break

            val candidateStart = maxOf(start + minimumStep, breakPoint - safeOverlap)
            val nextStart = alignChunkStart(normalizedText, candidateStart)
            start = nextStart.coerceIn(start + 1, normalizedText.length)
        }

        return chunks
    }

    private fun findBreakPoint(text: String, start: Int, end: Int): Int {
        val preferredSearchStart = start + (end - start) / 2

        // Try each separator from most preferred to least.
        // We prefer a break in the latter half so we keep chunk sizes reasonably stable.
        for (separator in separators) {
            val idx = text.lastIndexOf(separator, startIndex = end - 1, ignoreCase = false)
            if (idx in preferredSearchStart until end) {
                return idx + separator.length
            }
        }

        return end
    }

    private fun alignChunkStart(text: String, candidateStart: Int): Int {
        if (candidateStart <= 0 || candidateStart >= text.length) return candidateStart

        var start = candidateStart

        // If overlap lands in the middle of a word, advance to the next boundary
        // so we don't create suffix chunks like "reas", "eas", "as", ...
        if (text[start - 1].isWordCharacter() && text[start].isWordCharacter()) {
            while (start < text.length && text[start].isWordCharacter()) {
                start++
            }
        }

        while (start < text.length && text[start].isWhitespace()) {
            start++
        }

        return start
    }

    private fun Char.isWordCharacter(): Boolean = isLetterOrDigit() || this == '_'
}
