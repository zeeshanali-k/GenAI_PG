package com.devscion.genai_pg_kmp.domain.model

import com.devscion.genai_pg_kmp.ui.state.DocumentState

data class ChatHistoryItem(
    val id: String,
    val message: String,
    val isLLMResponse: Boolean,
    val isLoading: Boolean,
    val attachments: List<DocumentState> = emptyList()
)
