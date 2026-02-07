package com.devscion.genai_pg_kmp.data.model_managers

import android.util.Log
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.LlamatikPathProviderAndroid
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.InferenceBackend
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class LiteRTLM_ModelManager(
    private val llamatikPathProviderAndroid: LlamatikPathProviderAndroid,
    override var ragManager: RAGManager
) : LLMModelManager {

    private var conversation: Conversation? = null
    override var systemMessage: String? = null
    private var engine: Engine? = null

    override suspend fun loadModel(model: Model): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val engineConfig = EngineConfig(
                    modelPath = llamatikPathProviderAndroid.getPath(model.id)
                        ?: return@withContext false,
                    backend = model.backend.toLiteRTLMBackend(),
                    maxNumTokens = model.maxTokens,
                    visionBackend = Backend.GPU,
                    // optional: Pick a writable dir. This can improve 2nd load time.
                    // cacheDir = "/tmp/" or context.cacheDir.path (for Android)
                )
                engine = Engine(engineConfig)
                engine?.initialize()
                val conversationConfig = ConversationConfig(
                    systemMessage = systemMessage?.let {
                        Message.model(Contents.of(Content.Text(it)))
                    },
                    samplerConfig = SamplerConfig(
                        topK = model.topK,
                        topP = model.topP.toDouble(),
                        temperature = model.temperature.toDouble(),
                        seed = model.randomSeed,
                    ),
                )

                conversation = engine?.createConversation(conversationConfig)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override suspend fun sendPromptToLLM(
        inputPrompt: String,
        attachments: List<PlatformFile>?
    ): Flow<ChunkedModelResponse> =
        withContext(Dispatchers.IO) {
            callbackFlow {
                if (engine == null) {
                    throw IllegalStateException("Engine must be initialized")
                }
                val contents = mutableListOf<Content>()
                contents.add(Content.Text(inputPrompt))

                attachments?.filter { it.type == MediaType.IMAGE && it.bytes != null }
                    ?.forEach { file ->
                        Log.d("LiteRT_LMModelManager", "file-> $file")

                        contents.add(Content.ImageBytes(file.bytes!!))
                    }
                conversation?.sendMessageAsync(
                    Contents.of(contents),
                    callback = object : MessageCallback {
                        override fun onMessage(message: Message) {
                            Log.d(
                                "LiteRT_LMModelManager",
                                "message-> $message"
                            )
                            message.contents.contents.forEach {
                                Log.d(
                                    "LiteRT_LMModelManager",
                                    "content-> $it"
                                )
                                if (it is Content.Text) {
                                    trySend(
                                        ChunkedModelResponse(
                                            false,
                                            it.text
                                        )
                                    )
                                }
                            }
                        }

                        override fun onDone() {
                            Log.d(
                                "LiteRT_LMModelManager",
                                "onDone"
                            )
                            trySend(
                                ChunkedModelResponse(
                                    true,
                                    ""
                                )
                            )
                        }

                        override fun onError(throwable: Throwable) {
                            Log.d(
                                "LiteRT_LMModelManager",
                                "onError"
                            )
                            throwable.printStackTrace()
                            trySend(
                                ChunkedModelResponse(
                                    true,
                                    ""
                                )
                            )
                        }
                    })

                awaitClose {
                    this.trySend(
                        ChunkedModelResponse(
                            true,
                            ""
                        )
                    )
                    this@LiteRTLM_ModelManager.close()
                }
            }
        }

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String
    ): Boolean {
        return ragManager.loadEmbeddingModel(embeddingModelPath, tokenizerPath)
    }

    override fun close() {
        engine?.close()
        engine = null
        conversation?.close()
        conversation = null
    }

    override fun stopResponseGeneration() {
        conversation?.cancelProcess()
    }


    fun InferenceBackend.toLiteRTLMBackend(): Backend = when (this) {
        InferenceBackend.CPU -> Backend.CPU
        InferenceBackend.GPU -> Backend.GPU
    }
}