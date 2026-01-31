package com.devscion.genai_pg_kmp.domain.model

data class ChatHistoryItem(
    val id: String,
    val message: String,
    val isLLMResponse: Boolean,
    val isLoading: Boolean
)
