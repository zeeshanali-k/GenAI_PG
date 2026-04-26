package com.devscion.genai_pg_kmp.data.database.dao

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction

@Dao
interface VectorEmbeddingsDao {

    // Insert an embedding (stored as BLOB — sqlite-vec's binary format)
    @RawQuery
    @Transaction
    suspend fun insertEmbedding(query: RoomRawQuery): Long

    // KNN search — returns matching document IDs with distances
    @RawQuery
    suspend fun searchNearest(query: RoomRawQuery): List<VectorSearchResult>

}

data class VectorSearchResult(
    val rowid: Long,
    val distance: Float,
    val content: String,
    val file_name: String,
)