package com.devscion.genai_pg_kmp.data.repository

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.data.database.dao.ChatDao
import com.devscion.genai_pg_kmp.data.database.dao.MessageDao
import com.devscion.genai_pg_kmp.data.database.entity.Attachment
import com.devscion.genai_pg_kmp.data.database.entity.ChatEntity
import com.devscion.genai_pg_kmp.data.database.entity.MessageEntity
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.PlatformFile
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
) : ChatRepository {

    private val logger = Logger.withTag("ChatRepositoryImpl")

    override fun getChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMessages(chatId: String): Flow<List<ChatHistoryItem>> =
        messageDao.getMessages(chatId).mapLatest { messages -> messages.mapToChatHistoryItemList() }

    override suspend fun getMessagesList(chatId: String): List<ChatHistoryItem> =
        messageDao.getMessagesList(chatId).mapToChatHistoryItemList()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun List<MessageEntity>.mapToChatHistoryItemList() = map { msg ->
        val attachments = if (msg.attachments.isNotEmpty()) {
            msg.attachments.map {
                logger.d { "GetMessages-> attachment: $it" }
                DocumentState(
                    id = it.id,
                    title = it.title,
                    type = it.type,
                    isSent = true,
                    platformFile = PlatformFile(
                        name = it.title,
                        pathOrUri = it.uri,
                        bytes = if (it.type in listOf(
                                MediaType.AUDIO,
                                MediaType.IMAGE
                            )
                        ) pathProvider.getContentByteArray(it.uri)
                        else null,
                        content = null,
                        type = it.type,
                    ), content = if (it.type == MediaType.DOCUMENT) pathProvider.getContentText(
                        it.uri
                    ) ?: ""
                    else ""
                )
            }
        } else emptyList()
        ChatHistoryItem(
            id = msg.id,
            message = msg.content,
            isLLMResponse = msg.isFromUser.not(),
            attachments = attachments
        )
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
    ): String {
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
        return id
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
