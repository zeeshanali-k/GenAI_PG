package com.devscion.genai_pg_kmp.domain

import androidx.compose.runtime.Stable

interface FilePicker {
    suspend fun pickMedia(): PlatformFile?
    suspend fun pickFile(extensions: List<String>): PlatformFile?
}

enum class MediaType {
    IMAGE, DOCUMENT, AUDIO, MODEL
}

@Stable
data class PlatformFile(
    val name: String,
    val content: String?,
    val pathOrUri: String = "",
    val type: MediaType,
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PlatformFile

        if (name != other.name) return false
        if (content != other.content) return false
        if (pathOrUri != other.pathOrUri) return false
        if (type != other.type) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + pathOrUri.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}
