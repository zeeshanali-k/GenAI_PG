package com.devscion.genai_pg_kmp.data

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.LlamatikPathProvider
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.Model
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class LlamatikModelManager(
    private val llamatikPathProvider: LlamatikPathProvider,
) : LLMModelManager {
    override var systemMessage: String? = null

    override suspend fun loadModel(model: Model): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val path = llamatikPathProvider.getPath(model.id).also {
                    Logger.d("LlamatikModelManager") {
                        "Path-> $it"
                    }
                    println("LlamatikModelManager-> loadModel-> Path: $it")
                } ?: return@withContext false
                val isLoaded = LlamaBridge.initGenerateModel(path)
                return@withContext isLoaded
            } catch (e: Exception) {
                Logger.d("LlamatikModelManager") {
                    "Error-> ${e.message} :: ${e.cause}"
                }
                return@withContext false
            }
        }
    }

    override fun close() {
        LlamaBridge.shutdown()
    }

    override fun stopResponseGeneration() {
        LlamaBridge.nativeCancelGenerate()
    }

    override suspend fun sendPromptToLLM(inputPrompt: String): Flow<ChunkedModelResponse> =
        callbackFlow {
            withContext(Dispatchers.IO) {
                LlamaBridge.generateStreamWithContext(
                    systemMessage ?: "", "", inputPrompt,
                    object : GenStream {
                        override fun onDelta(text: String) {
                            println("LlamatikModelManager-> sendPromptToLLM-> onDelta: $text")
                            trySend(
                                ChunkedModelResponse(
                                    isDone = false,
                                    chunk = text
                                )
                            )
                        }

                        override fun onComplete() {
                            println("LlamatikModelManager-> sendPromptToLLM-> onComplete")
                            trySend(
                                ChunkedModelResponse(
                                    isDone = true,
                                    chunk = ""
                                )
                            )
                        }

                        override fun onError(message: String) {
                            println("LlamatikModelManager-> sendPromptToLLM-> onError: $message")
                            trySend(
                                ChunkedModelResponse(
                                    isDone = true,
                                    chunk = ""
                                )
                            )
                        }

                    })
            }
            awaitClose {

            }
        }
}