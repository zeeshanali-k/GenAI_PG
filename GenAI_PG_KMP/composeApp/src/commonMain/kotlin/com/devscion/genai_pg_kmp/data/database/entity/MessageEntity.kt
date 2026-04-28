package com.devscion.genai_pg_kmp.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.document.DocumentTextParser
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.ui.state.DocumentState
import kotlin.time.Clock

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val attachments: List<Attachment>,
)

suspend fun MessageEntity.toChatHistoryItem(
    pathProvider: ModelPathProvider,
    documentTextParser: DocumentTextParser,
): ChatHistoryItem {
    val msg = this
    val attachments = if (msg.attachments.isNotEmpty()) {
        msg.attachments.map {
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
                ), content = if (it.type == MediaType.DOCUMENT) {
                    documentTextParser.parse(it.uri, it.title) ?: ""
                }
                else ""
            )
        }
    } else emptyList()
    return ChatHistoryItem(
        id = msg.id,
        message = msg.content,
        isLLMResponse = msg.isFromUser.not(),
        attachments = attachments
    )
}


data class Attachment(
    val id: String,
    val uri: String,
    val type: MediaType,
    val title: String,
)
