package com.devscion.genai_pg_kmp.data.rag

import android.content.Context
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGDocumentChunk
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import com.devscion.genai_pg_kmp.domain.repository.VectorDBRepository
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * RAG manager implementation for Android MediaPipe.
 */
class MediaPipeRAGManager(
    private val context: Context,
    private val modelPathProvider: ModelPathProvider,
    private val vectorDBRepository: VectorDBRepository,
) : RAGManager {

    private val splitter = SimpleDocumentSplitter()
    private val logger = Logger.withTag("AIEdgeRAGManager")
    private var embedder: TextEmbedder? = null
    private var isModelLoaded = false

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = modelPathProvider.resolvePath(embeddingModelPath)
            logger.d { "Loading embedding model: $embeddingModelPath :: $path" }
            if (embedder != null) {
                return@withContext true
            }
            embedder = TextEmbedder.createFromFile(
                context,
                path
            )
            logger.d { "Embedding model loaded" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Exception while loading embedding model" }
            isModelLoaded = false
            false
        }
    }

    override suspend fun indexDocument(document: RAGDocument) {
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Indexing document: ${document.id}" }

                // Split document into chunks
                val chunks = splitter.splitOnCharacter(document.content)
                logger.d { "Split into ${chunks.size} chunks" }
//                val future = chainConfig!!.semanticMemory.getOrNull()
//                    ?.recordBatchedMemoryItems(ImmutableList.copyOf(chunks))
                chunks.forEachIndexed { index, chunk ->
                    val embedding = embedder!!.embed(chunk).embeddingResult().embeddings()

                    logger.d { "Embedding Result chunk $index: ${chunk.length} :: ${embedding?.size}" }
                    embedding.firstOrNull()?.floatEmbedding()?.copyOf(768)?.let {
                        vectorDBRepository.addEmbeddings(
                            it,
                            ragDocumentChunk = RAGDocumentChunk(
                                docId = Clock.System.now().toEpochMilliseconds(),
                                filename = document.metadata["title"]!!,
                                chunk = chunk,
                                chatId = document.metadata["chat_id"]!!
                            )
                        )
                    }
                }
                logger.d { "Successfully indexed document ${document.id}" }
            } catch (e: Exception) {
                logger.e(e) {
                    "Failed to index document: ${document.id} :: ${e.cause} :: ${e.message}"
                }
            }
        }
    }

    override suspend fun retrieveContext(query: String, topK: Int): String =
        withContext(Dispatchers.IO) {
            try {
                logger.d { "Retrieving context for query (topK=$topK)" }

                val promptEmbedding =
                    embedder!!.embed(query).embeddingResult().embeddings().firstOrNull()
                        ?.floatEmbedding() ?: return@withContext ""

                val retrievedResponse = vectorDBRepository.retrieveText(
                    promptEmbedding.copyOf(768)
                )

                logger.d { "Retrieved $retrievedResponse" }
                if (retrievedResponse.isEmpty()) ""
                else "[Context:\n$retrievedResponse"
            } catch (e: Exception) {
                logger.e(e) { "Failed to retrieve context" }
                ""
            }
        }

    override suspend fun clearIndex() {
        //TODO: implement
        embedder?.close()
        logger.d { "Cleared RAG index" }
    }

    override suspend fun isInitialized(): Boolean {
        return isModelLoaded
    }
}
