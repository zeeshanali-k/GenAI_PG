@file:OptIn(ExperimentalUuidApi::class, ExperimentalContracts::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.domain.model.Platform
import com.devscion.genai_pg_kmp.domain.model.androidOptions
import com.devscion.genai_pg_kmp.domain.model.iOSOptions
import com.devscion.genai_pg_kmp.ui.state.ChatHistory
import com.devscion.genai_pg_kmp.ui.state.ChatUIState
import com.devscion.genai_pg_kmp.ui.state.DocumentState
import com.devscion.genai_pg_kmp.ui.state.DocumentsState
import com.devscion.genai_pg_kmp.ui.state.EmbeddingProgress
import com.devscion.genai_pg_kmp.ui.state.ModelManagerError
import com.devscion.genai_pg_kmp.ui.state.ModelManagerState
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
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
    private val platformDetailProvider: PlatformDetailProvider,
    private val filePicker: FilePicker
) : ViewModel(), KoinComponent {

    private lateinit var permissionsController: PermissionsController

    private var llmResponseJob: Job? = null
    val inputFieldState = TextFieldState()

    private val modelManagerState = MutableStateFlow(
        ModelManagerState(
            modelManagerOptions = loadModelManagerOptions()
        )
    )
    private val chatHistoryState = MutableStateFlow(ChatHistory())
    private val documentsState = MutableStateFlow(DocumentsState())

    val uiState = combine(
        modelManagerState,
        chatHistoryState,
        documentsState
    ) { modeManagerState, chatHistory, documents ->
        if (modeManagerState.modelManagerOptions.isEmpty()) {
            ChatUIState.Loading
        } else {
            ChatUIState.Success(
                chatHistory = chatHistory,
                modelManagerState = modeManagerState,
                documentsState = documents
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
            // Embed any pending documents before sending
            embedPendingDocuments()

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

            // Use RAG if documents are attached, otherwise regular prompt
            val flow = if (documentsState.value.documents.isNotEmpty()) {
                modelManager?.sendPromptWithRAG(message)
            } else {
                modelManager?.sendPromptToLLM(message)
            }

            flow?.collectLatest {
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

    fun setPermissionsController(controller: PermissionsController) {
        this.permissionsController = controller
    }

    fun onAttachMedia() {
        viewModelScope.launch {
            try {
                if (::permissionsController.isInitialized) {
                    try {
                        // Request storage permission if needed. 
                        // Note: For simple file picking (ACTION_GET_CONTENT), strictly speaking, 
                        // explicit storage permission isn't always needed for the picked URI 
                        // as the system grants temporary access. However, usually good practice or required for some scopes.
                        // Using STORAGE as a generic placeholder.
                        // For Android 13+, Moko maps STORAGE to READ_EXTERNAL_STORAGE (legacy) or throws? 
                        // We'll wrap in try-catch in case permission is not applicable/needed or defined in Manifest
                        permissionsController.providePermission(Permission.STORAGE)
                    } catch (e: Exception) {
                        Logger.w("ChatViewModel") { "Permission request failed or rejected: ${e.message}" }
                    }
                }

                val file = filePicker.pickDocument()
                if (file != null) {
                    attachDocument(file.name, file.content)
                }
            } catch (e: Exception) {
                Logger.e("ChatViewModel", e) { "Failed to pick document" }
            }
        }
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
                    else ModelManagerError.FailedToLoadModel,
                    selectedLLM = if (isLoaded) it.selectedLLM else null,
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

    // RAG Document Management

    fun attachDocument(title: String, content: String) {
        val document = DocumentState(
            title = title,
            content = content,
            isEmbedded = false
        )
        documentsState.update {
            it.copy(documents = it.documents + document)
        }
    }

    fun removeDocument(documentId: String) {
        val document = documentsState.value.documents.firstOrNull { it.id == documentId }
        documentsState.update {
            it.copy(documents = it.documents.filter { doc -> doc.id != documentId })
        }
        document?.takeIf { it.isEmbedded }?.let {
            viewModelScope.launch {
                embedPendingDocuments(isReEmbedding = true)
            }
        }
    }

    private suspend fun embedPendingDocuments(isReEmbedding: Boolean = false) {
        val pendingDocs =
            if (isReEmbedding) documentsState.value.documents else documentsState.value.documents.filter { !it.isEmbedded }
        if (pendingDocs.isEmpty()) return

        documentsState.update {
            it.copy(
                isEmbedding = true,
                embeddingProgress = EmbeddingProgress(0, pendingDocs.size)
            )
        }

        try {
            modelManagerState.value.selectedLLM!!.let {
                modelManager!!.loadEmbeddingModel(
                    it.embeddingModel,
                    it.tokenizerModel,
                )
            }
            pendingDocs.forEachIndexed { index, doc ->
                val ragDocument = com.devscion.genai_pg_kmp.domain.rag.RAGDocument(
                    id = doc.id,
                    content = doc.content,
                    metadata = mapOf("title" to doc.title)
                )

                modelManager!!.indexDocument(ragDocument)

                // Mark document as embedded
                documentsState.update { state ->
                    state.copy(
                        documents = state.documents.map {
                            if (it.id == doc.id) it.copy(isEmbedded = true) else it
                        },
                        embeddingProgress = EmbeddingProgress(index + 1, pendingDocs.size)
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("ChatViewModel", e) { "Failed to embed documents" }
        } finally {
            documentsState.update {
                it.copy(
                    isEmbedding = false,
                    embeddingProgress = null
                )
            }
        }
    }

}