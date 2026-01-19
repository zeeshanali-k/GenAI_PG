package com.devscion.genai_pg_kmp.domain

import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import kotlinx.coroutines.flow.Flow

interface ModelManager {

    val systemMessage: String?

    suspend fun loadModel()

    fun close()

    suspend fun sendPromptToLLM(inputPrompt: String) : Flow<ChunkedModelResponse>

}