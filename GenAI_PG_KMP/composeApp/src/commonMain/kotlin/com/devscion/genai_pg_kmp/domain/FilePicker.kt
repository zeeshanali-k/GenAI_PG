package com.devscion.genai_pg_kmp.domain

interface FilePicker {
    suspend fun pickDocument(): PickedFile?
}

data class PickedFile(
    val name: String,
    val content: String // Taking content as string for now as per previous implementation
)
