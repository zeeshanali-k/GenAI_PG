package com.devscion.genai_pg_kmp.data.model_managers

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.devscion.genai_pg_kmp.domain.LLMModelManager
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
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LiteRTLM_ModelManager(
    override var ragManager: RAGManager
) : LLMModelManager, KoinComponent {

    private var conversation: Conversation? = null
    override var systemMessage: String? = null
    private var engine: Engine? = null
    private var modelPfd: ParcelFileDescriptor? = null
    private val context by lazy { getKoin().get<android.content.Context>() }

    override suspend fun loadModel(model: Model): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val resolvedModelPath =
                    resolveModelPath(model.localPath ?: return@withContext false)
                        ?: return@withContext false
                val engineConfig = EngineConfig(
                    modelPath = resolvedModelPath,
                    backend = model.backend.toLiteRTLMBackend(),
                    maxNumTokens = model.maxTokens,
                    visionBackend = if (model.isVisionEnabled) Backend.GPU else null,
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
                            this@callbackFlow.close()
                        }

                        override fun onError(throwable: Throwable) {
                            Log.d(
                                "LiteRT_LMModelManager",
                                "onError"
                            )
                            throwable.printStackTrace()
                            this@callbackFlow.close()
                        }
                    })

                awaitClose {
                    this.trySend(
                        ChunkedModelResponse(
                            true,
                            ""
                        )
                    )
                }
            }
        }

    override fun close() {
        engine?.close()
        engine = null
        conversation?.close()
        conversation = null
        modelPfd?.close()
        modelPfd = null
    }

    override suspend fun stopResponseGeneration() {
        withContext(Dispatchers.IO) {
            conversation?.cancelProcess()
        }
    }


    fun InferenceBackend.toLiteRTLMBackend(): Backend = when (this) {
        InferenceBackend.CPU -> Backend.CPU
        InferenceBackend.GPU -> Backend.GPU
    }

    private fun resolveModelPath(pathOrUri: String): String? {
        if (!pathOrUri.startsWith("content://")) return pathOrUri
        val uri = Uri.parse(pathOrUri)
        return try {
            modelPfd?.close()
            modelPfd = context.contentResolver.openFileDescriptor(uri, "r")
            val pfd = modelPfd ?: return null
            val procPath = "/proc/self/fd/${pfd.fd}"
            if (canOpenProcFd(procPath)) {
                procPath
            } else {
                val displayName = queryDisplayName(uri) ?: "model"
                modelPfd?.close()
                modelPfd = null
                copyUriToAppStorage(uri, displayName)
            }
        } catch (e: Exception) {
            Log.e("LiteRTLM_ModelManager", "Failed to open model Uri: ${e.message}")
            null
        }
    }

    private fun canOpenProcFd(procPath: String): Boolean {
        return try {
            FileInputStream(procPath).use { }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use { c ->
            if (c != null && c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return null
    }

    private fun copyUriToAppStorage(uri: Uri, fileName: String): String? {
        return try {
            val targetDir =
                context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
            if (!targetDir.exists() && !targetDir.mkdirs()) return null
            val targetFile = File(targetDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e("LiteRTLM_ModelManager", "Failed to copy model: ${e.message}")
            null
        }
    }
}
