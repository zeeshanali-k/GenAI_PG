package com.devscion.genai_pg_kmp.data.model_managers

import android.content.Context
import com.devscion.genai_pg_kmp.data.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.ModelManager
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.toLiteRTLMBackend
import com.google.ai.edge.litertlm.Content
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
    private val context: Context,
    private val model: Model,
    override val systemMessage: String? = null,
) : ModelManager {

    private var engine: Engine? = null

    override suspend fun loadModel() {
        withContext(Dispatchers.IO) {
            val engineConfig = EngineConfig(
                modelPath = model.modelPath(context),
                backend = model.backend.toLiteRTLMBackend(),
                maxNumTokens = model.maxTokens,
                // optional: Pick a writable dir. This can improve 2nd load time.
                // cacheDir = "/tmp/" or context.cacheDir.path (for Android)
            )
            engine = Engine(engineConfig)
            engine?.initialize()
        }
    }

    override suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse> =
        callbackFlow {
            if (engine == null) {
                throw IllegalStateException("Engine must be initialized")
            }
            val conversationConfig = ConversationConfig(
                systemMessage = systemMessage?.let {
                    Message.of(Content.Text(it))
                },
                samplerConfig = SamplerConfig(
                    topK = model.topK,
                    topP = model.topP.toDouble(),
                    temperature = model.temperature.toDouble(),
                    seed = model.randomSeed,
                ),
            )

            val conversation = engine?.createConversation(conversationConfig)
            conversation?.sendMessageAsync(Message.of(Content.Text(inputPrompt)))
                ?.catch { }
                ?.collectLatest { message ->
                    message.contents.forEach {
                        if (it is Content.Text)
                            this.send(
                                ChunkedModelResponse(
                                    false,
                                    it.text
                                )
                            )
                    }
                }

            awaitClose {
                this@LiteRTLM_ModelManager.close()
            }
        }

    override fun close() {
        engine?.close()
        engine = null
    }

    companion object {
        val MODELS_LIST = listOf(
            Model(
                id = "gemma-3n-E2B-it-int4.litertlm",
                name = "Gemma 3n 2B",
                size = 3660,
            ),
        )
    }
}