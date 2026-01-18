package com.devscion.llmspg.data.model_managers

import android.content.Context
import com.devscion.llmspg.data.ChunkedModelResponse
import com.devscion.llmspg.data.Model
import com.devscion.llmspg.domain.ModelManager
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import com.google.ai.edge.litertlm.Content
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

class LiteRTLM_ModelManager(
    private val model: Model,
    private val context: Context,
) : ModelManager {

    override val responseFlow: MutableSharedFlow<ChunkedModelResponse> =
        MutableSharedFlow()

    private var engine: Engine? = null

    override suspend fun loadModel() {
        val engineConfig = EngineConfig(
            modelPath = model.modelPath(context),
            backend = Backend.CPU,
            // optional: Pick a writable dir. This can improve 2nd load time.
            // cacheDir = "/tmp/" or context.cacheDir.path (for Android)
        )
        engine = Engine(engineConfig)
        engine?.initialize()
    }

    override suspend fun sendPromptToLLM(inputPrompt: String) {
        val conversationConfig = ConversationConfig(
            systemMessage = Message.of(Content.Text("You are a helpful assistant.")),
//            initialMessages = listOf(
//                Message.user("What is the capital city of the United States?"),
//                Message.model("Washington, D.C."),
//            ),
            samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.8),
        )

        val conversation = engine?.createConversation(conversationConfig)
        conversation?.sendMessageAsync(Message.of(Content.Text("You are a helpful assistant.")))
            ?.catch { }
            ?.collectLatest { message ->
                message.contents.forEach {
                    if (it is Content.Text)
                        responseFlow.emit(
                            ChunkedModelResponse(
                                false,
                                it.text
                            )
                        )
                }
            }
    }

}