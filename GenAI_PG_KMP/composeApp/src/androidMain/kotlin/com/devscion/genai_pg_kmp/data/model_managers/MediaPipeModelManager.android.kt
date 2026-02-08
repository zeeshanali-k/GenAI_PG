package com.devscion.genai_pg_kmp.data.model_managers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.devscion.genai_pg_kmp.data.rag.MediaPipeRAGManager
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.LlamatikPathProvider
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.InferenceBackend
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.PromptTemplates
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPipeModelManager(
    private val context: Context,
    private val llamatikPathProvider: LlamatikPathProvider,
    override var ragManager: RAGManager
) : LLMModelManager {

    private val sessionOptions: LlmInferenceSession.LlmInferenceSessionOptions.Builder =
        LlmInferenceSession.LlmInferenceSessionOptions.builder()
    val llmInferenceOptions: LlmInference.LlmInferenceOptions.Builder =
        LlmInference.LlmInferenceOptions.builder()
            .setMaxNumImages(5)
    override var systemMessage: String? = null
    private var inferenceSession: LlmInferenceSession? = null
    private var llmInference: LlmInference? = null

    override suspend fun loadModel(model: Model): Boolean {
        val modelPath = llamatikPathProvider.getPath(model.id) ?: return false
        Log.d("MediaPipeModelManager", "modelPath -> $modelPath")
        if (modelPath.isEmpty()) {
            return false
        }
        return withContext(Dispatchers.IO) {
            try {
                llmInferenceOptions.setModelPath(modelPath)
                    .setMaxTopK(model.topK)
                    .setMaxTokens(model.maxTokens)
                    .setPreferredBackend(model.backend.toMediaPipeBackend())
                llmInference = LlmInference.createFromOptions(
                    context,
                    llmInferenceOptions.build()
                )
                createSession(model)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun createSession(model: Model) {
        sessionOptions
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
                if (model.isVisionEnabled) {
                    setGraphOptions(
                        GraphOptions.builder()
                            .setEnableVisionModality(true)
                            .build()
                    )
                }
            }
        inferenceSession = LlmInferenceSession.createFromOptions(
            llmInference!!,
            sessionOptions.build()
        )
    }

    override suspend fun sendPromptToLLM(
        inputPrompt: String,
        attachments: List<PlatformFile>?
    ): Flow<ChunkedModelResponse> =
        withContext(Dispatchers.IO) {
            callbackFlow {
                if (inferenceSession == null) {
                    throw IllegalStateException("Engine must be initialized")
                }
                attachments?.filter { it.type == MediaType.IMAGE && it.bytes != null }
                    ?.forEach { file ->
                        val bitmap = BitmapFactory.decodeByteArray(file.bytes, 0, file.bytes!!.size)
                        if (bitmap != null) {
                            try {
                                inferenceSession!!.addImage(BitmapImageBuilder(bitmap).build())
                            } catch (e: Exception) {
                                Log.e(
                                    "MediaPipeModelManager",
                                    "Failed to add image chunk: ${e.message}"
                                )
                            }
                        }
                    }

                inferenceSession!!.addQueryChunk(inputPrompt)
                inferenceSession!!.generateResponseAsync { subText, isDone ->
                    Log.d("LLMResponse", "ModelManager-> $isDone :: $subText")
                    trySend(
                        ChunkedModelResponse(
                            isDone = isDone,
                            chunk = subText,
                        )
                    ).onFailure {
                        Log.d("LLMResponse", "ModelManager failed-> $isDone")
                    }
                    if (isDone) {
                        close()
                    }
                }

                awaitClose {
                    trySend(ChunkedModelResponse(isDone = true, chunk = ""))
                }
            }
        }

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String
    ): Boolean {
        return ragManager.loadEmbeddingModel(
            llamatikPathProvider.getPath(embeddingModelPath)
                ?: throw IllegalArgumentException("Invalid model path"),
            llamatikPathProvider.getPath(tokenizerPath)
                ?: throw IllegalArgumentException("Invalid model path"),
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        inferenceSession?.cancelGenerateResponseAsync()
        inferenceSession?.close()
        inferenceSession = null
        llmInference?.close()
        llmInference = null

        GlobalScope.launch {//TODO: remove GlobalScope
            (ragManager as? MediaPipeRAGManager)?.clearIndex()
        }
        // Don't nullify ragManager as it might be reused or is injected
        // ragManager = null
    }

    override fun stopResponseGeneration() {
        inferenceSession?.cancelGenerateResponseAsync()
    }


    fun InferenceBackend.toMediaPipeBackend(): LlmInference.Backend = when (this) {
        InferenceBackend.CPU -> LlmInference.Backend.CPU
        InferenceBackend.GPU -> LlmInference.Backend.GPU
    }

}