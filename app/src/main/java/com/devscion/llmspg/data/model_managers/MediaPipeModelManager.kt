package com.devscion.llmspg.data.model_managers

import android.content.Context
import android.util.Log
import com.devscion.llmspg.data.ChunkedModelResponse
import com.devscion.llmspg.data.Model
import com.devscion.llmspg.domain.ModelManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

class MediaPipeModelManager(
    private val context: Context,
    private val model: Model
) : ModelManager {

    private var llmInference: LlmInference? = null
    override val responseFlow = MutableSharedFlow<ChunkedModelResponse>()

    override suspend fun loadModel() {
        val modelPath = model.modelPath(context)
        Log.d("ModelManager", "modelPath-> $modelPath")
        if (modelPath.isEmpty()) {
            return
        }
        withContext(Dispatchers.IO) {
            val taskOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTopK(64)
                .build()
            llmInference = LlmInference.createFromOptions(context, taskOptions)
        }
    }

    override suspend fun sendPromptToLLM(inputPrompt: String) {
        if (llmInference == null) return
        withContext(Dispatchers.IO) {
            llmInference?.generateResponseAsync(
                inputPrompt
            ) { subText, isDone ->
                responseFlow.tryEmit(
                    ChunkedModelResponse(
                        isDone = isDone,
                        chunk = subText,
                    )
                )
            }
        }
    }

    fun subscribeResponseFlow(): SharedFlow<ChunkedModelResponse> = responseFlow

    companion object Companion {
        val MODELS_LIST = listOf(
            Model(
                id = "gemma-3n-E2B-it-int4.litertlm",
                name = "Gemma 3n-2B",
                size = 3660,
            )
        )
    }

}