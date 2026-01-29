@file:OptIn(ExperimentalForeignApi::class)

package com.devscion.genai_pg_kmp.data.model_managers

import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.SwiftModelManager
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.Model
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class MediaPipeModelManager(
    private val swiftModelManager: SwiftModelManager,
) : LLMModelManager {

    override var systemMessage: String? = null

    override suspend fun loadModel(model: Model) {
        println("LLMResponse loadModel-> Loading Path")
        withContext(Dispatchers.IO) {
            swiftModelManager.loadModel(model)
            println("LLMResponse loadModel-> Loading completed")
        }
    }

    override fun close() {
        swiftModelManager.close()
    }

    override fun stopResponseGeneration() {
        swiftModelManager.stopResponseGeneration()
    }

    override suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse> =
        withContext(Dispatchers.IO) {
            callbackFlow {
                swiftModelManager.generateResponseAsync(
                    inputPrompt,
                    { chunk ->
                        println("LLMResponse chunk-> $chunk")
                        chunk.let {
                            trySend(ChunkedModelResponse(isDone = false, chunk = it))
                        }
                    }
                ) { completeResponse, error ->
                    if (error != null) {
                        trySend(ChunkedModelResponse(isDone = true, chunk = ""))
                        println("LLMResponse error-> $error")
                        close()
                        return@generateResponseAsync
                    }
                    println("LLMResponse completeResponse: $completeResponse")
                    trySend(ChunkedModelResponse(isDone = true, chunk = ""))
                    close()
                }

                awaitClose {
                    trySend(ChunkedModelResponse(isDone = true, chunk = ""))
                }
            }
        }
}