package com.devscion.genai_pg_kmp.data.repository

import androidx.room.RoomRawQuery
import androidx.room.useWriterConnection
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.data.database.AppDatabase
import com.devscion.genai_pg_kmp.data.database.dao.VectorEmbeddingsDao
import com.devscion.genai_pg_kmp.domain.rag.RAGDocumentChunk
import com.devscion.genai_pg_kmp.domain.repository.VectorDBRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class VectorDBRepositoryImpl(
    private val vectorEmbeddingsDao: VectorEmbeddingsDao,
    private val db: AppDatabase,
) : VectorDBRepository {
    private val topK = 6L
    private val logger = Logger.withTag("VectorDBRepository")
    private val ioDispatcher = Dispatchers.IO
    override suspend fun addEmbeddings(
        embeddings: FloatArray,
        ragDocumentChunk: RAGDocumentChunk
    ) {
        withContext(ioDispatcher) {
            val json = embeddings.joinToString(",", "[", "]")
            logger.d { "Inserting embeddings:  ${embeddings.size} :: ${ragDocumentChunk.chunk}" }
            db.useWriterConnection { connection ->
                val isSuccess = connection.usePrepared(
                    "INSERT OR REPLACE INTO doc_embeddings(rowid, embedding, content, file_name, chat_id) VALUES (?, ?, ?, ?, ?)"
                ) { stmt ->
                    stmt.bindLong(1, ragDocumentChunk.docId)
                    stmt.bindText(2, json)
                    stmt.bindText(3, ragDocumentChunk.chunk)
                    stmt.bindText(4, ragDocumentChunk.filename)
                    stmt.bindText(5, ragDocumentChunk.chatId)
                    stmt.step()
                }
                logger.d { "Inserted embeddings: $isSuccess" }
            }
        }
    }

    override suspend fun retrieveEmbeddings(
        promptEmbeddings: FloatArray,
        chatId: String,
    ): FloatArray =
        withContext(ioDispatcher) {

            logger.d { "Retrieving embeddings: ${promptEmbeddings.size}" }
            val json = promptEmbeddings.joinToString(",", "[", "]")
            val query = RoomRawQuery(
                sql = """
                SELECT rowid, content, file_name, distance
                FROM doc_embeddings
                WHERE embedding MATCH ?
                AND chat_id = ?
                ORDER BY distance
                LIMIT ?
            """
            ) { stmt ->
                stmt.bindText(1, json)
                stmt.bindText(2, chatId)
                stmt.bindLong(3, topK)
            }
            val result = vectorEmbeddingsDao.searchNearest(query)
            logger.d { "Retrieved embeddings: $result" }
            floatArrayOf()//TODO
        }

    override suspend fun hasChatDocumentEmbeddings(
        chatId: String,
        fileName: String
    ): Boolean {
        return try {
            val query = RoomRawQuery(
                sql = """
                SELECT embedding
                FROM doc_embeddings WHERE
                chat_id = ?
                AND file_name = ?
                LIMIT ?
            """
            ) { stmt ->
                stmt.bindText(1, chatId)
                stmt.bindText(2, fileName)
                stmt.bindLong(3, 2)
            }
            val result = vectorEmbeddingsDao.getChatDocumentEmbeddings(query)
            logger.d { "Retrieved embeddings: $result" }
            result.isNotEmpty()
        } catch (e: Exception) {
            logger.d { "error retrieving embeddings: ${e.message} :: ${e.cause}" }
            false
        }
    }

    override suspend fun retrieveText(
        promptEmbeddings: FloatArray,
        chatId: String,
    ): String =
        withContext(ioDispatcher) {

            logger.d { "Retrieving embeddings: ${promptEmbeddings.size}" }
            val json = promptEmbeddings.joinToString(",", "[", "]")
            val query = RoomRawQuery(
                sql = """
                SELECT rowid, content, file_name, distance
                FROM doc_embeddings
                WHERE embedding MATCH ?
                AND chat_id = ?
                ORDER BY distance
                LIMIT ?
            """
            ) { stmt ->
                stmt.bindText(1, json)
                stmt.bindText(2, chatId)
                stmt.bindLong(3, topK)
            }
            val result = vectorEmbeddingsDao.searchNearest(query)
            logger.d {
                "Retrieved embeddings: ${result.size} :: [${
                    result.map { it.distance }.joinToString(", ")
                }]"
            }
            result.forEach { r ->
                if (result.firstOrNull { it.distance == r.distance } != null) {
                    logger.d { "Retrieved embeddings: ${r.rowid} :: ${r.distance} :: ${r.content}" }
                }
            }
            result.joinToString { it.content + "\n" }
        }

    override suspend fun retrieveAllText(chatId: String): String {
        val query = RoomRawQuery(
            sql = """
                SELECT rowid, content, file_name, distance
                FROM doc_embeddings
                WHERE chat_id = ?
            """
        ) { stmt ->
            stmt.bindText(1, chatId)
        }
        val result = vectorEmbeddingsDao.searchNearest(query)
        logger.d {
            "Retrieved embeddings: ${result.size}"
        }

        return result.joinToString { it.content }
    }

}