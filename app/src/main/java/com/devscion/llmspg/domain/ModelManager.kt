package com.devscion.llmspg.domain

import com.devscion.llmspg.data.ChunkedModelResponse
import kotlinx.coroutines.flow.MutableSharedFlow

interface ModelManager {

    val responseFlow: MutableSharedFlow<ChunkedModelResponse>

    suspend fun loadModel()

    suspend fun sendPromptToLLM(inputPrompt: String)

}