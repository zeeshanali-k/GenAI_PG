package com.devscion.genai_pg_kmp.data.rag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Represents a chunk of text with its embedding vector.
 */
data class ChunkWithEmbedding(
    val text: String,
    val embedding: FloatArray,
    val documentId: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ChunkWithEmbedding

        if (text != other.text) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (documentId != other.documentId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + documentId.hashCode()
        return result
    }
}

/**
 * Simple in-memory vector store using cosine similarity for search.
 * Suitable for small to medium document collections.
 */
class InMemoryVectorStore {

    private val chunks = mutableListOf<ChunkWithEmbedding>()

    /**
     * Add a chunk with its embedding to the store.
     *
     * @param text The text content
     * @param embedding The embedding vector
     * @param documentId Optional document identifier
     */
    fun add(text: String, embedding: FloatArray, documentId: String = "") {
        chunks.add(ChunkWithEmbedding(text, embedding, documentId))
    }

    /**
     * Search for the most similar chunks using cosine similarity.
     *
     * @param queryEmbedding The query embedding vector
     * @param topK Number of top results to return
     * @return List of the most similar chunks
     */
    suspend fun search(queryEmbedding: FloatArray, topK: Int): List<ChunkWithEmbedding> =
        withContext(Dispatchers.Default) {
            if (chunks.isEmpty()) return@withContext emptyList()

            // Calculate cosine similarity for each chunk
            val similarities = chunks.map { chunk ->
                val similarity = cosineSimilarity(queryEmbedding, chunk.embedding)
                chunk to similarity
            }

            // Sort by similarity (descending) and take top K
            return@withContext similarities
                .sortedByDescending { it.second }
                .take(topK.coerceAtMost(chunks.size))
                .map { it.first }
        }

    /**
     * Clear all chunks from the store.
     */
    fun clear() {
        chunks.clear()
    }

    /**
     * Get the total number of chunks stored.
     */
    fun size(): Int = chunks.size

    /**
     * Calculate cosine similarity between two vectors.
     *
     * @param a First vector
     * @param b Second vector
     * @return Cosine similarity value between -1 and 1
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0f) dotProduct / denominator else 0f
    }

    /**
     * Simple embedding generation using basic hashing.
     */
    fun generateSimpleEmbedding(text: String): FloatArray {
        val words = text.lowercase().split(Regex("\\s+"))
        val embedding = FloatArray(128)

        words.forEach { word ->
            val hash = word.hashCode()
            val index = kotlin.math.abs(hash % 128)
            embedding[index] += 1f
        }

        // Normalize
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }

        return embedding
    }

    fun generateAndStoreEmbedding(text: String) {
        val splitter = SimpleDocumentSplitter()
        val splitText = splitter.split(text)
        splitText.forEach { chunk ->
            val embeddings = generateSimpleEmbedding(chunk)
            add(chunk, embeddings)
        }
    }
}
