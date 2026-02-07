package com.devscion.genai_pg_kmp.ui.state

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a document attached for RAG.
 */
@OptIn(ExperimentalUuidApi::class)
data class DocumentState(
    val id: String = Uuid.random().toString(),
    val title: String,
    val content: String,
    val isEmbedded: Boolean = false,
    val size: Long = content.length.toLong()
)

/**
 * State for document management in chat.
 */
data class DocumentsState(
    val documents: List<DocumentState> = emptyList(),
    val isEmbedding: Boolean = false,
    val embeddingProgress: EmbeddingProgress? = null
)

/**
 * Progress information for document embedding.
 */
data class EmbeddingProgress(
    val current: Int,
    val total: Int
) {
    val percentage: Float
        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f
}
