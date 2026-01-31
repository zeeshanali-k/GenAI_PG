@file:OptIn(ExperimentalUuidApi::class, ExperimentalContracts::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.domain.model.Platform
import com.devscion.genai_pg_kmp.domain.model.androidOptions
import com.devscion.genai_pg_kmp.domain.model.iOSOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import kotlin.contracts.ExperimentalContracts
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel(
    private val platformDetailProvider: PlatformDetailProvider
) : ViewModel(), KoinComponent {

    private var llmResponseJob: Job? = null
    val inputFieldState = TextFieldState()

    private val modelManagerState = MutableStateFlow(
        ModelManagerState(
            modelManagerOptions = loadModelManagerOptions()
        )
    )
    private val chatHistoryState = MutableStateFlow(ChatHistory())

    val uiState = combine(
        modelManagerState,
        chatHistoryState,
    ) { modeManagerState, chatHistory ->
        if (modeManagerState.modelManagerOptions.isEmpty()) {
            ChatUIState.Loading
        } else {
            ChatUIState.Success(
                chatHistory = chatHistory,
                modelManagerState = modeManagerState
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatUIState.Loading)

    private var modelManager: LLMModelManager? = null

    private fun loadModelManagerOptions() = when (platformDetailProvider.getPlatform()) {
        Platform.ANDROID -> androidOptions()
        Platform.IOS -> iOSOptions()
    }

    fun onSend() {
        if (llmResponseJob?.isActive ?: false || inputFieldState.text.isEmpty()) return
        if (modelManagerState.value.selectedManager == null) {
            modelManagerState.update {
                it.copy(
                    modelManagerError = ModelManagerError.InvalidRuntime
                )
            }
            return
        }
        if (modelManagerState.value.selectedLLM == null) {
            modelManagerState.update {
                it.copy(
                    modelManagerError = ModelManagerError.InvalidModel
                )
            }
            return
        }
        llmResponseJob = viewModelScope.launch {
            addUserMessage()
            val message = inputFieldState.text.toString()
            resetInputField()
            val responseId = Uuid.generateV7().toString()
            chatHistoryState.value.history.add(
                ChatHistoryItem(
                    id = responseId,
                    message = "Generating...",
                    isLLMResponse = true,
                    isLoading = true
                )
            )
            val responseIndex = chatHistoryState.value.history.lastIndex
            modelManagerState.update {
                it.copy(
                    isGeneratingResponse = true
                )
            }
            modelManager?.sendPromptToLLM(message)
                ?.collectLatest {
                    val chatMessage = chatHistoryState.value.history[responseIndex]

                    chatHistoryState.value.history[responseIndex] = chatMessage.copy(
                        message = if (chatMessage.isLoading) it.chunk else chatMessage.message + it.chunk,
                        isLoading = false
                    )
                    Logger.d("ChatViewModel") {
                        "chunk -> ${it.chunk} :: ${it.isDone}"
                    }
                    if (it.isDone) {
                        cleanup()
                    }
                }
        }
    }

    private fun cleanup() {
        llmResponseJob?.cancel()
        llmResponseJob = null
        modelManagerState.update {
            it.copy(
                isGeneratingResponse = false
            )
        }
    }

    fun stopGeneratingResponse() {
        modelManager!!.stopResponseGeneration()
    }

    private fun resetInputField() {
        inputFieldState.edit {
            this.replace(0, originalText.length, "")
        }
    }

    private fun addUserMessage() {
        chatHistoryState.value.history.add(
            ChatHistoryItem(
                id = Uuid.generateV7().toString(),
                message = inputFieldState.text.toString(),
                isLLMResponse = false,
                isLoading = false
            )
        )
    }

    fun onAttachMedia() {

    }

    fun onRuntimeSelected(modelManagerOption: ModelManagerOption) {
        modelManagerState.update {
            it.copy(
                selectedManager = modelManagerOption,
                llmList = Model.models(
                    modelManagerOption.type,
                    platformDetailProvider.getPlatform()
                )
            )
        }

        modelManager?.close()
        modelManager = null
        modelManager = getKoin().get(named(modelManagerOption.type))
    }

    fun onLLMSelected(model: Model) {
        modelManagerState.update {
            it.copy(
                selectedLLM = model,
                isLoadingModel = true
            )
        }
        viewModelScope.launch {
            val isLoaded = modelManager!!.loadModel(model)
            modelManagerState.update {
                it.copy(
                    isLoadingModel = false,
                    modelManagerError = if (isLoaded) it.modelManagerError
                    else ModelManagerError.FailedToLoadModel
                )
            }
        }
    }

    fun toggleManagerSelection() {
        modelManagerState.update {
            it.copy(
                showManagerSelection = it.showManagerSelection.not()
            )
        }
    }

    fun resetError() {

        modelManagerState.update {
            it.copy(
                modelManagerError = ModelManagerError.Initial
            )
        }
    }

    fun toggleModelSelection() {
        modelManagerState.update {
            it.copy(
                showModelSelection = it.showModelSelection.not()
            )
        }
    }

    override fun onCleared() {
        cleanup()
        modelManager?.close()
    }


}