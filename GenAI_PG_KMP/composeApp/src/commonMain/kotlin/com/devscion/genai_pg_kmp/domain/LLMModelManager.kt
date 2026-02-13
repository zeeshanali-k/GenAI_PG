package com.devscion.genai_pg_kmp.domain

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import kotlinx.coroutines.flow.Flow

interface LLMModelManager {

    var systemMessage: String?

    suspend fun loadModel(model: Model): Boolean

    fun close()

    suspend fun stopResponseGeneration()

    suspend fun sendPromptToLLM(
        inputPrompt: String,
        attachments: List<PlatformFile>
    ): Flow<ChunkedModelResponse>

    // RAG Support
    var ragManager: RAGManager

    suspend fun indexDocument(document: RAGDocument) {
        ragManager.indexDocument(document)
    }

    suspend fun loadEmbeddingModel(embeddingModelPath: String, tokenizerPath: String): Boolean {
        return ragManager.loadEmbeddingModel(
            embeddingModelPath,
            tokenizerPath,
        )
    }

    suspend fun sendPromptWithRAG(
        inputPrompt: String,
        topK: Int = 3,
        images: List<PlatformFile>? = null
    ): Flow<ChunkedModelResponse> {
        val context = ragManager.retrieveContext(inputPrompt, topK)
        Logger.d("LLMModelManager") {
            "RAG Context: $context"
        }
        val augmentedPrompt = if (context.isNotEmpty()) {
            buildPromptWithContext(inputPrompt, context)
        } else {
            inputPrompt
        }
        return sendPromptToLLM(augmentedPrompt, images ?: emptyList())
    }

    fun buildPromptWithContext(prompt: String, context: String): String {
        return """
            Context Information:
            $context
            Question: $prompt
            Answer the question based on the provided context. If the context doesn't contain relevant information, say so.
        """.trimMargin()
    }

}