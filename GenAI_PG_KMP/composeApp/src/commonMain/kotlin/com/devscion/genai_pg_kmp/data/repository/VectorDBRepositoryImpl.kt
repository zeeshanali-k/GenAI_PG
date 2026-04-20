package com.devscion.genai_pg_kmp.data.repository

import androidx.room.RoomRawQuery
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.data.database.dao.VectorEmbeddingsDao
import com.devscion.genai_pg_kmp.domain.rag.RAGDocumentChunk
import com.devscion.genai_pg_kmp.domain.repository.VectorDBRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class VectorDBRepositoryImpl(
    private val vectorEmbeddingsDao: VectorEmbeddingsDao,
) : VectorDBRepository {
    private val logger = Logger.withTag("VectorDBRepository")
    private val ioDispatcher = Dispatchers.IO
    override suspend fun addEmbeddings(
        embeddings: FloatArray,
        ragDocumentChunk: RAGDocumentChunk
    ) {
        withContext(ioDispatcher) {
            val json = embeddings.joinToString(",", "[", "]")
            val query = RoomRawQuery(
                sql = "INSERT OR REPLACE INTO doc_embeddings(rowid, embedding, content, file_name) VALUES (?, ?, ?, ?)"
            ) { stmt ->
                stmt.bindLong(1, ragDocumentChunk.docId)
                stmt.bindText(2, json)
                stmt.bindText(3, ragDocumentChunk.chunk)
                stmt.bindText(4, ragDocumentChunk.filename)
            }
            val result = vectorEmbeddingsDao.insertEmbedding(query)
            logger.d { "Inserted embedding with rowid: $result" }
        }
    }

    override suspend fun retrieveEmbeddings(promptEmbeddings: FloatArray): FloatArray =
        withContext(ioDispatcher) {
            val json = promptEmbeddings.joinToString(",", "[", "]")
            val query = RoomRawQuery(
                sql = """
                SELECT rowid, content, file_name, distance
                FROM doc_embeddings
                WHERE embedding MATCH ?
                ORDER BY distance
                LIMIT ?
            """
            ) { stmt ->
                stmt.bindText(1, json)
                stmt.bindLong(2, 5.toLong())
            }
            val result = vectorEmbeddingsDao.searchNearest(query)
            logger.d { "Retrieved embeddings: $result" }
            floatArrayOf()
        }

    override suspend fun retrieveText(promptEmbeddings: FloatArray): String =
        withContext(ioDispatcher) {
            ""
        }

}