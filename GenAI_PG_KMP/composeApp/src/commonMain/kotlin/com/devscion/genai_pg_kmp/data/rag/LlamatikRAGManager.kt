package com.devscion.genai_pg_kmp.data.rag

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * RAG manager implementation for Llamatik using native embedding capabilities.
 * Uses LlamaBridge.embed() for generating embeddings and in-memory vector store.
 */
class LlamatikRAGManager : RAGManager {

    private val vectorStore = InMemoryVectorStore()
    private val splitter = SimpleDocumentSplitter()
    private val logger = Logger.withTag("LlamatikRAGManager")
    private var isModelLoaded = false

    override suspend fun loadEmbeddingModel(modelPath: String, tokenizerPath: String): Boolean =
        withContext(Dispatchers.IO) {
            if (isModelLoaded) return@withContext true
            try {
                logger.d { "Loading embedding model: $modelPath" }
                isModelLoaded = LlamaBridge.initModel(modelPath)
                if (isModelLoaded) {
                    logger.d { "Embedding model loaded successfully" }
                } else {
                    logger.e { "Failed to load embedding model" }
                }
                isModelLoaded
            } catch (e: Exception) {
                logger.e(e) { "Exception while loading embedding model" }
                isModelLoaded = false
                false
            }
        }

    override suspend fun indexDocument(document: RAGDocument) = withContext(Dispatchers.IO) {
        try {
            logger.d { "Indexing document: ${document.id}" }

            // 1. Split document into chunks
            val chunks = splitter.split(document.content)
            logger.d { "Split into ${chunks.size} chunks" }

            // 2. Generate embeddings using Llamatik and store
            chunks.forEachIndexed { index, chunk ->
                try {
                    val embedding = LlamaBridge.embed(chunk)
                    vectorStore.add(chunk, embedding, document.id)
                    logger.d { "Indexed chunk $index/${chunks.size}" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to embed chunk $index" }
                }
            }

            logger.d { "Successfully indexed document ${document.id}. Total chunks in store: ${vectorStore.size()}" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to index document: ${document.id}" }
        }
    }

    override suspend fun retrieveContext(query: String, topK: Int): String =
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Retrieving context for query (topK=$topK)" }

                // 1. Generate embedding for query
                val queryEmbedding = LlamaBridge.embed(query)

                // 2. Search vector store
                val results = vectorStore.search(queryEmbedding, topK)

                logger.d { "Retrieved ${results.size} results" }

                // 3. Format results as context
                if (results.isEmpty()) {
                    ""
                } else {
                    results.mapIndexed { index, chunk ->
                        "[Context ${index + 1}]\n${chunk.text}"
                    }.joinToString("\n\n")
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to retrieve context" }
                ""
            }
        }

    override suspend fun clearIndex() {
        vectorStore.clear()
        logger.d { "Cleared RAG index" }
    }

    override suspend fun isInitialized(): Boolean {
        // Llamatik RAG is initialized if model is loaded
        return isModelLoaded
    }
}
