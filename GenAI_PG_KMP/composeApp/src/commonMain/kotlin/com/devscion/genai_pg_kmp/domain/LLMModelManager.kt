package com.devscion.genai_pg_kmp.domain

import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.Model
import kotlinx.coroutines.flow.Flow

interface LLMModelManager {

    var systemMessage: String?

    suspend fun loadModel(model: Model)

    fun close()

    fun stopResponseGeneration()

    suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse>

}