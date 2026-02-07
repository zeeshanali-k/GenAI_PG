package com.devscion.genai_pg_kmp.data.rag

/**
 * Simple document splitter that breaks text into chunks.
 * Splits by sentences and paragraphs with configurable overlap.
 */
class SimpleDocumentSplitter {
    
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
}
