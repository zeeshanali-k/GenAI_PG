package com.devscion.genai_pg_kmp.data.model_managers

import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.Model
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LiteRT_LMModelManager : LLMModelManager {

    override var systemMessage: String? = null

    override suspend fun loadModel(model: Model): Boolean {

        return false
    }

    override fun close() {
    }

    override fun stopResponseGeneration() {

    }

    override suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse> = flow {
    }
}