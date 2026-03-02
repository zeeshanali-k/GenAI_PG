package com.devscion.genai_pg_kmp.domain.repository

import com.devscion.genai_pg_kmp.data.database.entity.ChatEntity
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.ui.state.DocumentState
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<List<ChatEntity>>
    fun getMessages(chatId: String): Flow<List<ChatHistoryItem>>
    suspend fun getMessagesList(chatId: String): List<ChatHistoryItem>
    suspend fun createChat(title: String): String
    suspend fun sendMessage(
        chatId: String,
        content: String,
        isFromUser: Boolean,
        attachments: List<DocumentState> = emptyList()
    ): String

    suspend fun updateChatMessage(
        chatId: String,
        messageId: String,
        content: String,
    )

    suspend fun deleteChat(chatId: String)
}
