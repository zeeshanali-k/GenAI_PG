package com.devscion.genai_pg_kmp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
