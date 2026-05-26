package com.devscion.genai_pg_kmp.domain

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
import com.devscion.genai_pg_kmp.domain.model.MAX_TOKEN
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class RAGResponseStatus(val status: Int) {
    VALID(1), INVALID(-1), SUMMARIZE(0)
}

val RAG_VERIFICATION_SYSTEM_PROMPT =
    "You are analyzing a response/retrieved context from RAG pipeline for user prompt. Analyze the message and return just single digit in range [-1,  0, 1]. Don't return any text AT ALL. Just a single number from specified range." +
            " Return number ${RAGResponseStatus.SUMMARIZE.status} if the user asked for a summary/simplify/summarize, since we would like to fetch the whole document for summarization." +
            " Return number ${RAGResponseStatus.INVALID.status} if the response is not sufficient for the prompt to be answered in detail. " +
            " Return number ${RAGResponseStatus.VALID.status} if the response is detailed enough to response to user correctly. "

private val ragStatusPattern = Regex("""(?<![\d-])-?1(?!\d)|(?<![\d-])0(?!\d)""")
private val validRagStatuses = RAGResponseStatus.entries.map { it.status }.toSet()

fun parseRagResponseStatus(response: String?): Int {
    val trimmedResponse = response?.trim().orEmpty()
    return trimmedResponse.toIntOrNull()
        ?.takeIf { it in validRagStatuses }
        ?: ragStatusPattern.find(trimmedResponse)
            ?.value
            ?.toIntOrNull()
            ?.takeIf { it in validRagStatuses }
        ?: RAGResponseStatus.VALID.status
}

interface LLMRuntimeManager {

    var systemMessage: String?

    suspend fun loadModel(model: Model): Boolean

    fun close()

    suspend fun stopResponseGeneration()

    suspend fun sendPromptToLLM(
        inputPrompt: String,
        attachments: List<PlatformFile>
    ): Flow<ChunkedModelResponse>

    suspend fun getRagPromptResponse(prompt: String, ragResponse: String): Int

    // RAG Support
    var ragManager: RAGManager

    suspend fun indexDocument(document: RAGDocument) {
        ragManager.indexDocument(document)
    }

    suspend fun clearIndexedDocuments() {
        ragManager.clearIndex()
    }

    suspend fun loadEmbeddingModel(embeddingModelPath: String, tokenizerPath: String): Boolean {
        return ragManager.loadEmbeddingModel(
            embeddingModelPath,
            tokenizerPath,
        )
    }

    suspend fun sendPromptWithRAG(
        inputPrompt: String,
        topK: Int = 3,
        chatId: String,
        images: List<PlatformFile>? = null
    ): Flow<ChunkedModelResponse> {
        Logger.d("LLMModelManager") {
            "sendPromptWithRAG: $chatId :: $inputPrompt"
        }
        var context = ragManager.retrieveContext(
            query = inputPrompt,
            chatId = chatId,
            topK = topK
        )
        Logger.d("LLMModelManager") {
            "sendPromptWithRAG: context-> $context"
        }
        if (context.isNotBlank()) {
            when (val status = getRagPromptResponse(inputPrompt, context)) {
                RAGResponseStatus.VALID.status -> Unit

                RAGResponseStatus.SUMMARIZE.status -> {
                    context = ragManager.retrieveAllContext(chatId).ifBlank { context }
                }

                RAGResponseStatus.INVALID.status -> {
                    context += ragManager.retrieveContext(
                        query = inputPrompt,
                        chatId = chatId,
                        topK = topK
                    )
                }

                else -> {
                    Logger.d("LLMModelManager") {
                        "Unknown RAG verification status: $status"
                    }
                }
            }
        }
        val normalizedContext = normalizeContext(context, inputPrompt)
        Logger.d("LLMModelManager") {
            "RAG Context: $context"
        }
        val augmentedPrompt = if (context.isNotEmpty()) {
            buildPromptWithContext(inputPrompt, normalizedContext)
        } else {
            inputPrompt
        }
        return sendPromptToLLM(augmentedPrompt, images ?: emptyList())
    }

    fun normalizeContext(context: String, inputPrompt: String): String {
        var normalizedContext = context
        val splitContext = context.split(" ")
        val splitInputSize = context.split(" ")
        val cSize = splitContext.size
        val iSize = splitInputSize.size
        if (cSize + iSize > MAX_TOKEN) {
            normalizedContext = splitContext.take(MAX_TOKEN - iSize - 10).joinToString { " " }
        }
        return normalizedContext
    }

    fun insufficientRagContextFlow(): Flow<ChunkedModelResponse> {
        return flowOf(
            ChunkedModelResponse(
                isDone = false,
                chunk = "I could not find enough information in the attached document context to answer that reliably."
            ),
            ChunkedModelResponse(
                isDone = true,
                chunk = ""
            )
        )
    }

    fun buildPromptWithContext(prompt: String, context: String): String {
        return """
            User Provided Document Context:
            $context
            Question: $prompt
            Answer the question based on the user provided document's context. If the context doesn't contain relevant information, say so.
        """.trimMargin()
    }

}
