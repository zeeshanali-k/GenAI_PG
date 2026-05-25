package com.devscion.genai_pg_kmp.domain.repository

import com.devscion.genai_pg_kmp.domain.rag.RAGDocumentChunk

interface VectorDBRepository {

    suspend fun addEmbeddings(embeddings: FloatArray, ragDocumentChunk: RAGDocumentChunk)

    suspend fun retrieveEmbeddings(
        promptEmbeddings: FloatArray,
        chatId: String,
    ): FloatArray

    suspend fun hasChatDocumentEmbeddings(
        chatId: String,
        fileName: String,
    ): Boolean

    suspend fun retrieveText(
        promptEmbeddings: FloatArray,
        chatId: String,
    ): String

    suspend fun retrieveAllText(
        chatId: String,
    ): String

}