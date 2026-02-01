package com.devscion.genai_pg_kmp.data.model_managers

import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.LlamatikPathProviderAndroid
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.InferenceBackend
import com.devscion.genai_pg_kmp.domain.model.Model
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

class LiteRTLM_ModelManager(
    private val llamatikPathProviderAndroid: LlamatikPathProviderAndroid,
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
            } catch (_: Exception) {
                false
            }
        }
    }

    override suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse> =
        withContext(Dispatchers.IO) {
            callbackFlow {
                if (engine == null) {
                    throw IllegalStateException("Engine must be initialized")
                }
                conversation?.sendMessageAsync(Contents.of(Content.Text(inputPrompt)))
                    ?.catch {
                        send(
                            ChunkedModelResponse(
                                true,
                                ""
                            )
                        )
                    }
                    ?.collectLatest { message ->
                        message.contents.contents.forEach {
                            if (it is Content.Text) {
                                this.send(
                                    ChunkedModelResponse(
                                        false,
                                        it.text
                                    )
                                )
                            }
                        }
                    }

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