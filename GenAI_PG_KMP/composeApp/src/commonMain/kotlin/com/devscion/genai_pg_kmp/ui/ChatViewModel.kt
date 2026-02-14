@file:OptIn(ExperimentalUuidApi::class, ExperimentalContracts::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.LLMModelManager
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.ModelSettings
import com.devscion.genai_pg_kmp.domain.PlatformDetailProvider
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.model.EmbeddingModel
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntimeFeature
import com.devscion.genai_pg_kmp.domain.model.Platform
import com.devscion.genai_pg_kmp.domain.model.TokenizerModel
import com.devscion.genai_pg_kmp.domain.model.androidOptions
import com.devscion.genai_pg_kmp.domain.model.iOSOptions
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
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
    private val filePicker: FilePicker,
    private val modelSettings: ModelSettings
) : ViewModel(), KoinComponent {

    private lateinit var permissionsController: PermissionsController

    private var llmResponseJob: Job? = null
    val inputFieldState = TextFieldState()

    private val modelManagerState = MutableStateFlow(
        ModelManagerState(
            modelManagerOptions = loadModelManagerOptions(),
            embeddingModels = EmbeddingModel.EMBEDDING_MODELS.map {
                it.copy(localPath = modelSettings.getPath(it.id))
            },
            tokenizerModels = TokenizerModel.TOKENIZER_MODELS.map {
                it.copy(localPath = modelSettings.getPath(it.id))
            }
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

        val selectedLLM = modelManagerState.value.selectedLLM!!

        // Validation: Vision
        val isVisionNeeded = documentsState.value.documents.any { it.type == MediaType.IMAGE }
        if (isVisionNeeded && selectedLLM.features.contains(ModelManagerRuntimeFeature.VISION)
                .not()
        ) {
            modelManagerState.update { it.copy(ragError = "Selected model does not support Vision (images).") }
            return
        }
        // Validation: Audio

        val isAudioNeeded = documentsState.value.documents.any { it.type == MediaType.AUDIO }
        if (isAudioNeeded && selectedLLM.features.contains(ModelManagerRuntimeFeature.AUDIO)
                .not()
        ) {
            modelManagerState.update { it.copy(ragError = "Selected model does not support Audio.") }
            return
        }

        // Validation: RAG

        val isRAGNeeded = documentsState.value.documents.any { it.type == MediaType.DOCUMENT }
        if (isRAGNeeded) {
            if (modelManagerState.value.selectedEmbeddingModel == null || modelManagerState.value.selectedTokenizer == null) {
                modelManagerState.update { it.copy(ragError = "Please select an Embedding Model and Tokenizer for RAG.") }
                return
            }
        }

        // Validation: Local Paths
        if (selectedLLM.localPath == null) {
            modelManagerState.update { it.copy(ragError = "Please select the model file for ${selectedLLM.name}") }
            return
        }
        if (isRAGNeeded) {
            if (modelManagerState.value.selectedEmbeddingModel?.localPath == null) {
                modelManagerState.update { it.copy(ragError = "Please select the file for Embedding Model") }
                return
            }
            if (modelManagerState.value.selectedTokenizer?.localPath == null) {
                modelManagerState.update { it.copy(ragError = "Please select the file for Tokenizer") }
                return
            }
        }
        llmResponseJob = viewModelScope.launch {

            addUserMessage(documentsState.value.documents.toList())
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
            modelManagerState.update {
                it.copy(
                    isGeneratingResponse = true
                )
            }

            // Filter documents for RAG (Text only)
            val ragDocuments =
                documentsState.value.documents.filter { it.type == MediaType.DOCUMENT }
            // Filter attachments for Multimodal (Image, Audio)
            val images = documentsState.value.documents
                .filter { it.type != MediaType.DOCUMENT }
                .mapNotNull { it.platformFile }

            val flow = if (ragDocuments.isNotEmpty()) {
                embedPendingDocuments()
                resetDocumentsState()
                modelManager?.sendPromptWithRAG(message, images = images)
            } else {
                resetDocumentsState()
                modelManager?.sendPromptToLLM(
                    message,
                    images.takeIf { it.isNotEmpty() } ?: emptyList())
            }

            val responseIndex = chatHistoryState.value.history.lastIndex
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
                    if (it.isDone) {
                        cleanup()
                        // pendingImages.clear() // No longer needed
                    }
                }
            }
        }
    }

    private fun resetDocumentsState() {
        documentsState.update {
            it.copy(
                documents = emptyList()
            )
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
        viewModelScope.launch {
            modelManager!!.stopResponseGeneration()
        }
    }

    private fun resetInputField() {
        inputFieldState.edit {
            this.replace(0, originalText.length, "")
        }
    }

    private fun addUserMessage(attachments: List<DocumentState>) {
        chatHistoryState.value.history.add(
            ChatHistoryItem(
                id = Uuid.generateV7().toString(),
                message = inputFieldState.text.toString(),
                isLLMResponse = false,
                isLoading = false,
                attachments = attachments
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
                        permissionsController.providePermission(Permission.STORAGE)
                    } catch (e: Exception) {
                        Logger.w("ChatViewModel") { "Permission request failed or rejected: ${e.message}" }
                    }
                }

                val file = filePicker.pickMedia()
                if (file != null) {
                    when (file.type) {
                        MediaType.DOCUMENT -> attachDocument(file)
                        MediaType.IMAGE -> attachDocument(file)
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Logger.e("ChatViewModel", e) { "Failed to pick media" }
            }
        }
    }

    fun onRuntimeSelected(modelManagerOption: ModelManagerOption) {
        val models = Model.models(
            modelManagerOption.type,
            platformDetailProvider.getPlatform()
        )
        modelManagerState.update {
            it.copy(
                selectedManager = modelManagerOption,
                llmList = models.map {
                    it.copy(localPath = modelSettings.getPath(it.id))
                }
            )
        }

        modelManager?.close()
        modelManager = null
        modelManager = getKoin().get(named(modelManagerOption.type))
        resetCurrentChatHistory()
    }

    fun onModelSelected(model: Model) {
        modelManagerState.update {
            it.copy(
                selectedLLM = model,
                showModelSelection = false
            )
        }

        if (model.localPath.isNullOrEmpty()) {
            onFilePickForModel(model)
        } else {
            loadModel()
        }
    }

    fun onEmbeddingModelSelected(model: EmbeddingModel) {
        modelManagerState.update {
            it.copy(
                selectedEmbeddingModel = model,
                showEmbeddingSelection = false
            )
        }
    }

    fun onTokenizerSelected(tokenizer: TokenizerModel) {
        modelManagerState.update {
            it.copy(
                selectedTokenizer = tokenizer,
                showTokenizerSelection = false
            )
        }
    }

    fun onFilePickForModel(model: Model) {
        viewModelScope.launch {
            val file =
                filePicker.pickFile(modelManagerState.value.selectedLLM!!.supportedFormats.map { it.format })
            if (file != null) {
                modelManagerState.update { state ->
                    val updatedModel = model.copy(localPath = file.pathOrUri)
                    updatedModel.let { modelSettings.savePath(it.id, it.localPath) }
                    state.copy(
                        selectedLLM = updatedModel,
                    )
                }
                loadModel()
            }
        }
    }

    private fun loadModel() {
        modelManagerState.update {
            it.copy(
                isLoadingModel = true,
                modelManagerError = ModelManagerError.Initial
            )
        }
        viewModelScope.launch {
            val selectedModel = modelManagerState.value.selectedLLM!!
            val isLoaded = modelManager!!.loadModel(selectedModel)
            modelManagerState.update {
                it.copy(
                    isLoadingModel = false,
                    showModelSelection = isLoaded.not(),
                    modelManagerError = if (isLoaded) ModelManagerError.Initial
                    else ModelManagerError.FailedToLoadModel,
                )
            }
            if (isLoaded) {
                resetCurrentChatHistory()
            }
            if (isLoaded.not()) {
                resetModelPath(selectedModel)
                onFilePickForModel(selectedModel)
            }
        }
    }

    private fun resetCurrentChatHistory() {
        chatHistoryState.value.history.clear()
    }

    private fun resetModelPath(selectedModel: Model) {
        modelSettings.savePath(selectedModel.id, null)
        modelManagerState.update {
            it.copy(
                llmList = it.llmList?.map {
                    if (it.id == selectedModel.id) it.copy(
                        localPath = null
                    ) else it
                },
                selectedLLM = selectedModel.copy(localPath = null)
            )
        }
    }

    fun onFilePickForEmbedding(embeddingModel: EmbeddingModel) {
        viewModelScope.launch {
            val file = filePicker.pickFile(listOf("tflite", "bin"))
            if (file != null) {
                modelManagerState.update { state ->
                    val updatedModel =
                        embeddingModel.copy(localPath = file.pathOrUri)
                    updatedModel.let { modelSettings.savePath(it.id, it.localPath) }
                    state.copy(selectedEmbeddingModel = updatedModel)
                }
            }
        }
    }

    fun onFilePickForTokenizer(tokenizerModel: TokenizerModel) {
        viewModelScope.launch {
            val file = filePicker.pickFile(listOf("model", "bin"))
            if (file != null) {
                modelManagerState.update { state ->
                    val updatedTokenizer = tokenizerModel.copy(localPath = file.pathOrUri)
                    updatedTokenizer.let { modelSettings.savePath(it.id, it.localPath) }
                    state.copy(selectedTokenizer = updatedTokenizer)
                }
            }
        }
    }

    fun toggleEmbeddingSelection() {
        modelManagerState.update {
            it.copy(showEmbeddingSelection = !it.showEmbeddingSelection)
        }
    }

    fun toggleTokenizerSelection() {
        modelManagerState.update {
            it.copy(showTokenizerSelection = !it.showTokenizerSelection)
        }
    }

    fun resetRagError() {
        modelManagerState.update { it.copy(ragError = null) }
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
    fun attachDocument(file: PlatformFile) {
        val document = DocumentState(
            title = file.name,
            content = file.content ?: "", // Content might be empty for images
            isEmbedded = false,
            type = file.type,
            platformFile = if (file.type == MediaType.IMAGE) file else null
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
        document?.takeIf { it.isEmbedded && it.type == MediaType.DOCUMENT }?.let {
            viewModelScope.launch {
                embedPendingDocuments(isReEmbedding = true)
            }
        }
    }

    private suspend fun embedPendingDocuments(isReEmbedding: Boolean = false) {
        val pendingDocs = documentsState.value.documents.filter {
            if (isReEmbedding) {
                it.type == MediaType.DOCUMENT
            } else {
                it.isEmbedded.not() && it.type == MediaType.DOCUMENT
            }
        }

        if (pendingDocs.isEmpty()) return

        documentsState.update {
            it.copy(
                isEmbedding = true,
                embeddingProgress = EmbeddingProgress(0, pendingDocs.size)
            )
        }

        try {
            modelManagerState.value.let { state ->
                modelManager!!.loadEmbeddingModel(
                    state.selectedEmbeddingModel!!.localPath!!,
                    state.selectedTokenizer!!.localPath!!,
                )
            }
            pendingDocs.forEachIndexed { index, doc ->
                val ragDocument = RAGDocument(
                    id = doc.id,
                    content = doc.content,
                    metadata = mapOf("title" to doc.title)
                )

                modelManager!!.indexDocument(ragDocument)

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