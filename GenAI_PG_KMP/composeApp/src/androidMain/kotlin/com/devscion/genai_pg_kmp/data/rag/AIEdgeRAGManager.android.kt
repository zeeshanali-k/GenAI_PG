package com.devscion.genai_pg_kmp.data.rag

import android.content.Context
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.common.collect.ImmutableList
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * RAG manager implementation for Android ( MediaPipe & LiteRT-LM).
 *
 * NOTE: This is a simplified placeholder implementation.
 * To use the actual Google AI Edge RAG SDK, add the proper dependency
 * and implement using the SDK's VectorStore, DocumentSplitter, and EmbeddingGenerator.
 *
 * For now, this uses a simple in-memory approach similar to Llamatik.
 */
class AIEdgeRAGManager() : RAGManager {


    private var mediaPipeLanguageModel: MediaPipeLlmBackend? = null
    private var chainConfig: ChainConfig<String>? = null

    private var retrievalChain: RetrievalChain<String>? = null

    private var embedder: Embedder<String>? = null

    private val splitter = SimpleDocumentSplitter()
    private val logger = Logger.withTag("AIEdgeRAGManager")
    private var isModelLoaded = false


    /**
     * loadMediaPipeLanguageModel must be called before calling loadEmbeddingModel
     * @see loadEmbeddingModel
     * */
    suspend fun loadMediaPipeLanguageModel(
        context: Context,
        mediaPipeLanguageModelOptions: LlmInference.LlmInferenceOptions,
        mediaPipeLanguageModelSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.d { "Loading MediaPipeLanguageModel" }
            mediaPipeLanguageModel = MediaPipeLlmBackend(
                context, mediaPipeLanguageModelOptions,
                mediaPipeLanguageModelSessionOptions
            )
            return@withContext mediaPipeLanguageModel!!.initialize().get()
        } catch (e: Exception) {
            logger.e(e) { "Exception while loading MediaPipeLanguageModel" }
            return@withContext false
        }
    }

    override suspend fun loadEmbeddingModel(
        modelPath: String,
        tokenizerPath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.d { "Loading embedding model: $modelPath" }
            embedder = GeckoEmbeddingModel(
                modelPath,
                Optional.of(tokenizerPath),
                false
            )
            chainConfig = ChainConfig.create(
                mediaPipeLanguageModel, PromptBuilder(""),
                DefaultSemanticTextMemory(
                    // Gecko embedding model dimension is 768
                    SqliteVectorStore(768), embedder
                )
            )
            retrievalChain = RetrievalChain(chainConfig!!)
            logger.d { "Embedding model placeholder loaded" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Exception while loading embedding model" }
            isModelLoaded = false
            false
        }
    }

    override suspend fun indexDocument(document: RAGDocument) {
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Indexing document: ${document.id}" }

                // Split document into chunks
                val chunks = splitter.split(document.content)
                logger.d { "Split into ${chunks.size} chunks" }

                val future = chainConfig!!.semanticMemory.getOrNull()
                    ?.recordBatchedMemoryItems(ImmutableList.copyOf(chunks))
                future?.get()
                logger.d { "Successfully indexed document ${document.id}" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to index document: ${document.id}" }
            }
        }
    }

    override suspend fun retrieveContext(query: String, topK: Int): String =
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Retrieving context for query (topK=$topK)" }

                val retrievedResponse = retrievalChain!!(
                    RetrievalRequest.create(
                        query,
                        RetrievalConfig.create(topK, RetrievalConfig.TaskType.RETRIEVAL_QUERY)
                    )
                ).get()

                logger.d { "Retrieved ${retrievedResponse.entities.size} results" }

                if (retrievedResponse.entities.isEmpty()) {
                    ""
                } else {
                    retrievedResponse.entities.mapIndexed { index, chunk ->
                        "[Context ${index + 1}]\n${chunk.data}"
                    }.joinToString("\n\n")
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to retrieve context" }
                ""
            }
        }

    override suspend fun clearIndex() {
        logger.d { "Cleared RAG index" }
    }

    override suspend fun isInitialized(): Boolean {
        return isModelLoaded
    }

    /**
     * Simple embedding generation using basic hashing.
     */
//    private fun generateSimpleEmbedding(text: String): FloatArray {
//        val words = text.lowercase().split(Regex("\\s+"))
//        val embedding = FloatArray(128)
//
//        words.forEach { word ->
//            val hash = word.hashCode()
//            val index = kotlin.math.abs(hash % 128)
//            embedding[index] += 1f
//        }
//
//        // Normalize
//        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
//        if (norm > 0) {
//            for (i in embedding.indices) {
//                embedding[i] /= norm
//            }
//        }
//
//        return embedding
//    }
}

/**
 * Simple document splitter that splits by sentences/paragraphs
 */
private class SimpleDocumentSplitter(
    private val chunkSize: Int = 500,
    private val overlap: Int = 50
) {
    fun split(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            chunks.add(text.substring(start, end))
            start += chunkSize - overlap
        }

        return chunks
    }
}
/*

/**
 * VectorChunk represents a text chunk with its embedding
 */
private data class VectorChunk(
    val text: String,
    val embedding: FloatArray,
    val documentId: String,
    val score: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VectorChunk

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
*/
/**
 * Simple in-memory vector store using cosine similarity
 *//*
private class InMemoryVectorStore {
    private val vectors = mutableListOf<VectorChunk>()

    fun add(text: String, embedding: FloatArray, documentId: String) {
        vectors.add(VectorChunk(text, embedding, documentId))
    }

    fun search(queryEmbedding: FloatArray, topK: Int): List<VectorChunk> {
        return vectors
            .map { chunk ->
                val sim = cosineSimilarity(queryEmbedding, chunk.embedding)
                chunk.copy(score = sim)
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    fun clear() {
        vectors.clear()
    }

    fun size(): Int = vectors.size

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0f
        }
    }
}*/
