package com.devscion.genai_pg_kmp.domain.rag

/**
 * Represents a document to be indexed in the RAG system.
 *
 * @param id Unique identifier for the document
 * @param content The text content to be indexed
 * @param metadata Optional metadata associated with the document
 */
data class RAGDocument(
    val id: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)
