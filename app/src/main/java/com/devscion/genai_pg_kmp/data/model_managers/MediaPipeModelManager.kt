package com.devscion.genai_pg_kmp.data.model_managers

import android.content.Context
import android.util.Log
import com.devscion.genai_pg_kmp.domain.ModelManager
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.InferenceBackend
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.utils.modelPath
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
//            createSession()
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


    fun InferenceBackend.toMediaPipeBackend(): LlmInference.Backend = when (this) {
        InferenceBackend.CPU -> LlmInference.Backend.CPU
        InferenceBackend.GPU -> LlmInference.Backend.GPU
    }

    companion object Companion {
        val MODELS_LIST = listOf(
            Model(
                id = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
                name = "Gemma3 1B",
                size = 1070,
            ),
        )
//        GEMMA3_1B_IT_CPU(
//        path = "/data/local/tmp/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
//        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
//        licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
//        needsAuth = true,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 1.0f,
//        topK = 64,
//        topP = 0.95f
//    ),
//    GEMMA_3_1B_IT_GPU(
//        path = "/data/local/tmp/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
//        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
//        licenseUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
//        needsAuth = true,
//        preferredBackend = Backend.GPU,
//        thinking = false,
//        temperature = 1.0f,
//        topK = 64,
//        topP = 0.95f
//    ),
//    GEMMA_2_2B_IT_CPU(
//        path = "/data/local/tmp/Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Gemma2-2B-IT/resolve/main/Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "https://huggingface.co/litert-community/Gemma2-2B-IT",
//        needsAuth = true,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.6f,
//        topK = 50,
//        topP = 0.9f
//    ),
//    DEEPSEEK_R1_DISTILL_QWEN_1_5_B(
//        path = "/data/local/tmp/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = true,
//        temperature = 0.6f,
//        topK = 40,
//        topP = 0.7f
//    ),
//    LLAMA_3_2_1B_INSTRUCT(
//        path = "/data/local/tmp/Llama-3.2-1B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct/resolve/main/Llama-3.2-1B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct",
//        needsAuth = true,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.6f,
//        topK = 64,
//        topP = 0.9f
//    ),
//    LLAMA_3_2_3B_INSTRUCT(
//        path = "/data/local/tmp/Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct/resolve/main/Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct",
//        needsAuth = true,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.6f,
//        topK = 64,
//        topP = 0.9f,
//    ),
//    PHI_4_MINI_INSTRUCT(
//        path = "/data/local/tmp/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.6f,
//        topK = 40,
//        topP = 1.0f
//    ),
//    QWEN2_0_5B_INSTRUCT(
//        path = "/data/local/tmp/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.95f,
//        topK = 40,
//        topP = 1.0f
//    ),
//    QWEN2_1_5B_INSTRUCT(
//        path = "/data/local/tmp/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.95f,
//        topK = 40,
//        topP = 1.0f
//    ),
//    QWEN2_5_3B_INSTRUCT(
//        path = "/data/local/tmp/Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/Qwen2.5-3B-Instruct/resolve/main/Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.95f,
//        topK = 40,
//        topP = 1.0f
//    ),
//    SMOLLM_135M_INSTRUCT(
//        path = "/data/local/tmp/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.95f,
//        topK = 40,
//        topP = 1.0f
//    ),
//    TINYLLAMA_1_1B_CHAT_V1_0(
//        path = "/data/local/tmp/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
//        url = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
//        licenseUrl = "",
//        needsAuth = false,
//        preferredBackend = Backend.CPU,
//        thinking = false,
//        temperature = 0.95f,
//        topK = 40,
//        topP = 1.0f
//    ),
    }

}