package com.devscion.genai_pg_kmp.domain.repository

import com.devscion.genai_pg_kmp.domain.rag.RAGDocumentChunk

interface VectorDBRepository {

    suspend fun addEmbeddings(embeddings: FloatArray, ragDocumentChunk: RAGDocumentChunk)

    suspend fun retrieveEmbeddings(promptEmbeddings: FloatArray): FloatArray

    suspend fun retrieveText(promptEmbeddings: FloatArray): String

}