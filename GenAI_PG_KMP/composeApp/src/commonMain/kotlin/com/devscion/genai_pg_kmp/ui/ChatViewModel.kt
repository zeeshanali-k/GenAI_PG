package com.devscion.genai_pg_kmp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.domain.model.Platform
import com.devscion.genai_pg_kmp.domain.model.androidOptions
import com.devscion.genai_pg_kmp.domain.model.iOSOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

class ChatViewModel(
    private val platformDetailProvider: PlatformDetailProvider
) : ViewModel(), KoinComponent {

    private val modelManagerState = MutableStateFlow(ModelManagerState())
    private val chatHistoryState = MutableStateFlow(ChatHistory())
    private val userPromptState = MutableStateFlow("")

    val uiState = combine(
        modelManagerState,
        chatHistoryState,
        userPromptState
    ) { modeManagerState, chatHistory, userPrompt ->
        if (modeManagerState.modelManagerOptions.isEmpty()) {
            ChatUIState.Loading
        } else {
            ChatUIState.Success(
                userPrompt = userPrompt,
                chatHistory = chatHistory,
                modelManagerState = modeManagerState
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUIState.Loading)

    private var modelManager: LLMModelManager? = null

    init {
        loadModelManagerState()
    }

    private fun loadModelManagerState() {
        modelManagerState.update {
            it.copy(
                modelManagerOptions = when (platformDetailProvider.getPlatform()) {
                    Platform.ANDROID -> androidOptions()
                    Platform.IOS -> iOSOptions()
                }
            )
        }
    }

    fun onRuntimeSelected(modelManagerOption: ModelManagerOption) {
        modelManagerState.update {
            it.copy(
                selectedManager = modelManagerOption
            )
        }

        modelManager?.close()
        modelManager = getKoin().get(named(modelManagerOption.type))
    }

    fun toggleManagerSelection() {
        modelManagerState.update {
            it.copy(
                showManagerSelection = it.showManagerSelection.not()
            )
        }
    }

    fun onPromptUpdate(prompt: String) {
        userPromptState.value = prompt
    }


}