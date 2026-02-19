package com.devscion.genai_pg_kmp.data.database.converters

import androidx.room.TypeConverter
import com.devscion.genai_pg_kmp.data.database.entity.Attachment
import com.devscion.genai_pg_kmp.domain.MediaType

class AttachmentListConverter {

    private val FIELDS_SEPARATOR = "***"
    private val MODEL_SEPARATOR = "!!!"

    @TypeConverter
    fun attachmentsToString(attachments: List<Attachment>): String {
        return attachments.joinToString {
            "${it.id}${FIELDS_SEPARATOR}${it.type.name}${FIELDS_SEPARATOR}${it.uri}${FIELDS_SEPARATOR}${it.title}${MODEL_SEPARATOR}"
        }
    }

    @TypeConverter
    fun stringToAttachments(attachments: String): List<Attachment> {
        if (attachments.isEmpty()) return emptyList()
        val attachmentsList = mutableListOf<Attachment>()
        attachments.split(MODEL_SEPARATOR)
            .filter { it.isNotEmpty() }.forEach {
                val attachmentStr = it.split(FIELDS_SEPARATOR)

                attachmentsList.add(
                    Attachment(
                        id = attachmentStr[0],
                        type = MediaType.valueOf(attachmentStr[1]),
                        uri = attachmentStr[2],
                        title = attachmentStr[3]
                    )
                )
            }

        return attachmentsList
    }

}