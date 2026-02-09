package com.devscion.genai_pg_kmp.data.model_managers

import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Not supported for iOS yet.
 * */
class LiteRT_LMModelManager(
    override var ragManager: RAGManager
) : LLMModelManager {

    override var systemMessage: String? = null

    override suspend fun loadModel(model: Model): Boolean {

        return false
    }

    override fun close() {
    }

    override suspend fun stopResponseGeneration() {

    }

    override suspend fun sendPromptToLLM(
        inputPrompt: String,
        attachments: List<PlatformFile>?
    ): Flow<ChunkedModelResponse> = emptyFlow()

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String
    ): Boolean {
        return false
    }
}