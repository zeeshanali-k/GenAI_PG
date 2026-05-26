package com.devscion.genai_pg_kmp.data.repository

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.data.database.dao.ChatDao
import com.devscion.genai_pg_kmp.data.database.dao.MessageDao
import com.devscion.genai_pg_kmp.data.database.entity.Attachment
import com.devscion.genai_pg_kmp.data.database.entity.ChatEntity
import com.devscion.genai_pg_kmp.data.database.entity.MessageEntity
import com.devscion.genai_pg_kmp.data.database.entity.toChatHistoryItem
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.document.DocumentTextParser
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.repository.ChatRepository
import com.devscion.genai_pg_kmp.ui.state.DocumentState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val pathProvider: ModelPathProvider,
    private val documentTextParser: DocumentTextParser,
) : ChatRepository {

    private val logger = Logger.withTag("ChatRepositoryImpl")

    override fun getChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMessages(chatId: String): Flow<List<ChatHistoryItem>> = messageDao
        .getMessages(chatId)
        .mapLatest { messages -> messages.mapToChatHistoryItemList() }

    override suspend fun getMessagesList(chatId: String): List<ChatHistoryItem> = messageDao
        .getMessagesList(chatId)
        .mapToChatHistoryItemList()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun List<MessageEntity>.mapToChatHistoryItemList() = map { msg ->
        msg.toChatHistoryItem(pathProvider, documentTextParser)
    }

    override suspend fun createChat(title: String): String {
        val id = Uuid.random().toString()
        val chat = ChatEntity(id = id, title = title)
        chatDao.insertChat(chat)
        return id
    }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        isFromUser: Boolean,
        attachments: List<DocumentState>
    ): ChatHistoryItem {
        val attachmentData = mutableListOf<Attachment>()
        logger.d { "sendMessage: ${attachments.size}" }
        attachments.forEach { attachment ->
            logger.d { "sendMessage: map attachment-> ${attachment.platformFile}" }
            attachment.platformFile?.pathOrUri?.let {
                logger.d { "sendMessage: pathOrUri-> $it" }
                pathProvider.makeLocalCopy(it).takeIf { it.isNullOrEmpty().not() }?.let { uri ->
                    logger.d { "sendMessage: localPathOrUri-> $it" }
                    attachmentData.add(
                        Attachment(
                            id = attachment.id,
                            uri = uri,
                            type = attachment.type,
                            title = attachment.title,
                        )
                    )
                }
            }
        }

        val id = Uuid.random().toString()
        val message = MessageEntity(
            id = id,
            chatId = chatId,
            content = content,
            isFromUser = isFromUser,
            attachments = attachmentData
        )
        messageDao.insertMessage(message)
        return message.toChatHistoryItem(pathProvider, documentTextParser)
    }

    override suspend fun updateChatMessage(
        chatId: String,
        messageId: String,
        content: String
    ) = messageDao.updateMessage(chatId, messageId, content)


    override suspend fun deleteChat(chatId: String) {
        chatDao.deleteChatById(chatId)
    }
}
