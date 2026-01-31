package com.devscion.genai_pg_kmp.ui

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption

sealed class ChatUIState {
    data object Loading : ChatUIState()
    data object Error : ChatUIState()
    data class Success(
        val chatHistory: ChatHistory,
        val modelManagerState: ModelManagerState,
    ) : ChatUIState()
}

data class ModelManagerState(
    val modelManagerOptions: List<ModelManagerOption>,
    val llmList: List<Model>? = null,
    val selectedManager: ModelManagerOption? = null,
    val selectedLLM: Model? = null,
    val showManagerSelection: Boolean = false,
    val showModelSelection: Boolean = false,
    val isLoadingModel: Boolean = false,
    val isGeneratingResponse: Boolean = false,
    val modelManagerError: ModelManagerError = ModelManagerError.Initial,
)

sealed class ModelManagerError {
    data object Initial : ModelManagerError()
    data object InvalidRuntime : ModelManagerError()
    data object InvalidModel : ModelManagerError()
    data object FailedToLoadModel : ModelManagerError()
}

data class ChatHistory(
    val history: SnapshotStateList<ChatHistoryItem> = SnapshotStateList(),
)