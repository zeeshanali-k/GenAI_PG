package com.devscion.genai_pg_kmp.data.rag

import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.rag.RAGManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class MediaPipeRAGManager : RAGManager {

    private val vectorStore = InMemoryVectorStore()
    private val splitter = SimpleDocumentSplitter()

    override suspend fun loadEmbeddingModel(
        embeddingModelPath: String,
        tokenizerPath: String
    ): Boolean {
        //TODO: implement
        return true
    }

    override suspend fun indexDocument(document: RAGDocument) {
        try {
            val chunks = splitter.split(document.content)
            println("MediaPipeRAGManager-> indexDocument: chunks = ${chunks.size} :: $chunks")
            chunks.forEachIndexed { index, chunk ->
                try {
                    vectorStore.generateAndStoreEmbedding(chunk)
                } catch (e: Exception) {
                    println("MediaPipeRAGManager-> indexDocument: Failed to index chunk $index : $chunk")
                }
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun retrieveContext(query: String, topK: Int): String =
        withContext(Dispatchers.IO) {
            try {
                val queryEmbedding = vectorStore.generateSimpleEmbedding(query)
                val results = vectorStore.search(queryEmbedding, topK)
                println("MediaPipeRAGManager-> retrieveContext: Retrieved ${results.size} results")
                if (results.isEmpty()) {
                    ""
                } else {
                    results.mapIndexed { index, chunk ->
                        "[Context ${index + 1}]\n${chunk.text}"
                    }.joinToString("\n\n")
                }
            } catch (e: Exception) {
                println("MediaPipeRAGManager-> retrieveContext: Failed to retrieve context")
                ""
            }
        }

    override suspend fun clearIndex() {
        vectorStore.clear()
        println("Cleared RAG index")
    }

    override suspend fun isInitialized(): Boolean {
        return true
    }
}