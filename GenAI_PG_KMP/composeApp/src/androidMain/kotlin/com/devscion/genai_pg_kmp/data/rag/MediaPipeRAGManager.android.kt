package com.devscion.genai_pg_kmp.data.rag

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * RAG manager implementation for Android MediaPipe.
 */
class MediaPipeRAGManager() : RAGManager {


    private var chainConfig: ChainConfig<String>? = null

    private var retrievalChain: RetrievalChain<String>? = null

    private var embedder: Embedder<String>? = null

    private val splitter = SimpleDocumentSplitter()
    private val logger = Logger.withTag("AIEdgeRAGManager")
    private var isModelLoaded = false

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.d { "Loading embedding model: $embeddingModelPath" }
            if (embedder != null && chainConfig != null && retrievalChain != null) {
                return@withContext true
            }
            embedder = GeckoEmbeddingModel(
                embeddingModelPath,
                Optional.of(tokenizerPath),
                false
            )
            chainConfig = ChainConfig.create(getSemanticMemory())
            retrievalChain = RetrievalChain(chainConfig!!)
            logger.d { "Embedding model placeholder loaded" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Exception while loading embedding model" }
            isModelLoaded = false
            false
        }
    }

    private fun getSemanticMemory() = DefaultSemanticTextMemory(
        // Gecko embedding model dimension is 768
        SqliteVectorStore(768), embedder
    )

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
                        logger.d {
                            "retrievedResponse-> $index ${chunk.data}"
                        }
                        "[Context ${index + 1}]\n${chunk.data}"
                    }.joinToString("\n\n")
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to retrieve context" }
                ""
            }
        }

    override suspend fun clearIndex() {
        chainConfig = chainConfig?.toBuilder()?.setSemanticMemory(
            getSemanticMemory()
        )?.build()
        //TODO: test
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
