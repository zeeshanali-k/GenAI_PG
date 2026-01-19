package com.devscion.genai_pg_kmp.data.model_managers

import android.content.Context
import android.util.Log
import com.devscion.genai_pg_kmp.data.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.ModelManager
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.toMediaPipeBackend
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.PromptTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class MediaPipeModelManager(
    private val context: Context,
    private val model: Model,
    override val systemMessage: String? = null,
) : ModelManager {

    private var inferenceSession: LlmInferenceSession? = null
    private var llmInference: LlmInference? = null


    override suspend fun loadModel() {
        val modelPath = model.modelPath(context)
        Log.d("MediaPipeModelManager", "modelPath -> $modelPath")
        if (modelPath.isEmpty()) {
            return
        }
        withContext(Dispatchers.IO) {
            val taskOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTopK(model.topK)
                .setMaxTokens(model.maxTokens)
                .setPreferredBackend(model.backend.toMediaPipeBackend())
                .build()
            llmInference = LlmInference.createFromOptions(context, taskOptions)
            createSession()
        }
    }

    private fun createSession() {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(model.topK)
            .setTemperature(model.temperature)
            .setTopP(model.topP)
            .apply {
                if (systemMessage.isNullOrEmpty().not()) {
                    setPromptTemplates(
                        PromptTemplates.builder()
                            .setSystemPrefix(systemMessage)
                            .build()
                    )
                }
            }
            .build()
        inferenceSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
    }

    override suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse> =
        callbackFlow {
            if (llmInference == null) {
                throw IllegalStateException("Engine must be initialized")
            }
            withContext(Dispatchers.IO) {
                llmInference!!.generateResponseAsync(inputPrompt) { subText, isDone ->
                    Log.d("LLMResponse", "ModelManager-> $isDone :: $subText")
                    trySend(
                        ChunkedModelResponse(
                            isDone = isDone,
                            chunk = subText,
                        )
                    ).onFailure {
                        Log.d("LLMResponse", "ModelManager failed-> $isDone :: $subText")
                    }
                }
//            inferenceSession!!.addQueryChunk(inputPrompt)
//            inferenceSession!!.generateResponseAsync { subText, isDone ->
//                responseFlow.tryEmit(
//                    ChunkedModelResponse(
//                        isDone = isDone,
//                        chunk = subText,
//                    )
//                )
//            }
            }

            awaitClose {
                this@MediaPipeModelManager.close()
            }
        }

    override fun close() {
        inferenceSession?.close()
        inferenceSession = null
        llmInference?.close()
        llmInference = null
    }

    companion object Companion {
        val MODELS_LIST = listOf(
            Model(
                id = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
                name = "Gemma3 3 1B",
                size = 1770,
            ),
        )
    }

}