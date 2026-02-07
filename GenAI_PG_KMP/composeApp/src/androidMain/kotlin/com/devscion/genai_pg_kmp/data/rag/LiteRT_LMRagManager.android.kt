package com.devscion.genai_pg_kmp.data.rag

import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager

//TODO: implement using MediaPipe since LiteRT-LM doesn't have builtin RAG functionality
class LiteRT_LMRagManager : RAGManager {

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String
    ): Boolean {
        return false
    }

    override suspend fun indexDocument(document: RAGDocument) {

    }

    override suspend fun retrieveContext(query: String, topK: Int): String {
        return ""
    }

    override suspend fun clearIndex() {
    }

    override suspend fun isInitialized(): Boolean {
        return false
    }
}