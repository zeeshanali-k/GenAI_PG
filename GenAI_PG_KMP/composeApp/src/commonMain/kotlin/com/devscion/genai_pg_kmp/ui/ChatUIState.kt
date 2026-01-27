package com.devscion.genai_pg_kmp.ui

import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption

sealed class ChatUIState {
    data object Loading : ChatUIState()
    data object Error : ChatUIState()
    data class Success(
        val userPrompt: String,
        val chatHistory: ChatHistory,
        val modelManagerState: ModelManagerState = ModelManagerState(),
    ) : ChatUIState()
}

data class ModelManagerState(
    val modelManagerOptions: List<ModelManagerOption> = emptyList(),
    val selectedManager: ModelManagerOption? = null,
    val showManagerSelection: Boolean = false
)

data class ChatHistory(
    val history: List<ChatHistoryItem> = emptyList(),
    val streamingResponse: String? = null,
)