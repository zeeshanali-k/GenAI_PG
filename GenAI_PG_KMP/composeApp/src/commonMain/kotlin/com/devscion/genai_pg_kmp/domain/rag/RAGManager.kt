package com.devscion.genai_pg_kmp.domain.rag

/**
 * Unified interface for Retrieval-Augmented Generation (RAG) functionality.
 * This interface abstracts the RAG implementation details, allowing different
 * platforms and libraries to provide their own implementations.
 */
interface RAGManager {

    /**
     * Load and initialize the embedding model.
     * This must be called before indexing documents or retrieving context.
     *
     * @param modelPath Path to the embedding model file
     * @return true if model loaded successfully, false otherwise
     */
    suspend fun loadEmbeddingModel(modelPath: String, tokenizerPath: String): Boolean

    /**
     * Index a document for later retrieval.
     * The document will be split into chunks, embedded, and stored.
     *
     * @param document The document to index
     */
    suspend fun indexDocument(document: RAGDocument)

    /**
     * Retrieve relevant context from indexed documents based on a query.
     *
     * @param query The query to search for
     * @param topK Number of top results to return (default: 3)
     * @return Retrieved context as a formatted string
     */
    suspend fun retrieveContext(query: String, topK: Int = 3): String

    /**
     * Clear all indexed documents from the store.
     */
    suspend fun clearIndex()

    /**
     * Check if the RAG manager is properly initialized.
     *
     * @return true if initialized and ready to use
     */
    suspend fun isInitialized(): Boolean
}
