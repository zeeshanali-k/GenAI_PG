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
import com.devscion.genai_pg_kmp.domain.model.EmbeddingModel
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntimeFeature
import com.devscion.genai_pg_kmp.domain.model.Platform
import com.devscion.genai_pg_kmp.domain.model.TokenizerModel
import com.devscion.genai_pg_kmp.domain.model.androidOptions
import com.devscion.genai_pg_kmp.domain.model.iOSOptions
import com.devscion.genai_pg_kmp.domain.rag.RAGDocument
import com.devscion.genai_pg_kmp.domain.repository.ChatRepository
import com.devscion.genai_pg_kmp.ui.state.ChatHistory
import com.devscion.genai_pg_kmp.ui.state.ChatUIState
import com.devscion.genai_pg_kmp.ui.state.DocumentState
import com.devscion.genai_pg_kmp.ui.state.EmbeddingProgress
import com.devscion.genai_pg_kmp.ui.state.ModelManagerError
import com.devscion.genai_pg_kmp.ui.state.ModelManagerState
import com.devscion.genai_pg_kmp.ui.state.RAGDocumentsState
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.storage.STORAGE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val platformDetailProvider: PlatformDetailProvider,
    private val filePicker: FilePicker,
    private val modelSettings: ModelSettings,
    private val chatRepository: ChatRepository
) : ViewModel(), KoinComponent {
    private val logger = Logger.withTag("ChatVM")

    private lateinit var permissionsController: PermissionsController

    private var llmResponseJob: Job? = null
    val inputFieldState = TextFieldState()

    private val chats = chatRepository.getChats()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

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
    private val documentsState = MutableStateFlow(RAGDocumentsState())

    val uiState = combine(
        modelManagerState,
        chatHistoryState,
        documentsState,
        chats
    ) { modeManagerState, chatHistory, documents, chats ->
        if (modeManagerState.modelManagerOptions.isEmpty()) {
            ChatUIState.Loading
        } else {
            ChatUIState.Success(
                chatHistory = chatHistory.copy(chats = chats),
                modelManagerState = modeManagerState,
                documentsState = documents
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ChatUIState.Loading
    )

    private var modelManager: LLMModelManager? = null

    init {
        viewModelScope.launch {
            var lastChatId = chatHistoryState.value.currentChatId
            chatHistoryState.collectLatest { state ->
                val chatId = state.currentChatId
                if (chatId != null) {
                    chatRepository.getMessages(chatId)
                        .collectLatest { messages ->
                            chatHistoryState.value.chatMessages.clear()
                            chatHistoryState.value.chatMessages.addAll(messages)
                            logger.d { "collectLatest: ${messages.size} :: $lastChatId - $chatId" }
                            if (lastChatId != chatId && messages.isNotEmpty()) {
                                parseAndSetDocuments()
                                if (ensureEmbeddingsModelLoaded()) {
                                    embedPendingDocuments()
                                    resetDocumentsState()
                                }
                            }
                            lastChatId = chatId
                        }
                } else {
                    chatHistoryState.value.chatMessages.clear()
                    chatHistoryState.value.chatMessages.addAll(emptyList())
                }
            }
        }
    }

    private fun ensureEmbeddingsModelLoaded(): Boolean {
        return modelManager != null && modelManagerState.value.selectedEmbeddingModel != null
                && modelManagerState.value.selectedTokenizer != null
                && modelManagerState.value.selectedLLM != null
    }

    private fun parseAndSetDocuments() {
        val docs = mutableListOf<DocumentState>()
        chatHistoryState.value.chatMessages.forEach {
            if (it.attachments.isNotEmpty()) {
                logger.d {
                    "parseAndSetDocs: ${it.attachments}"
                }
            }
            docs.addAll(it.attachments)
        }
        documentsState.update {
            it.copy(
                documents = docs,
                isEmbedding = false,
            )
        }
    }

    private fun loadModelManagerOptions() = when (platformDetailProvider.getPlatform()) {
        Platform.ANDROID -> androidOptions()
        Platform.IOS -> iOSOptions()
    }

    fun onSend() {
        if (isSetupAndInputValid().not()) return
        llmResponseJob = viewModelScope.launch {
            addUserMessage(documentsState.value.documents.toList())
            val message = inputFieldState.text.toString()
            resetInputField()
            val chatResponseMessageId = chatRepository.sendMessage(
                chatId = chatHistoryState.value.currentChatId!!,
                content = "Generating...",
                isFromUser = false,
                attachments = emptyList(),
            )
            modelManagerState.update {
                it.copy(
                    isGeneratingResponse = true
                )
            }

            // Filter documents for RAG (Text only)
            documentsState.update {
                it.copy(
                    documents = it.documents.map { it.copy(isSent = true) }
                )
            }
            logger.d { "Docs: ${documentsState.value}" }
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
                val isRAGPrompt = chatHistoryState.value.chatMessages.any {
                    it.attachments.isNotEmpty() && it.attachments.any { attach ->
                        attach.type == MediaType.DOCUMENT
                    }
                }
                if (isRAGPrompt) {
                    modelManager?.sendPromptWithRAG(message, images = images)
                } else {
                    modelManager?.sendPromptToLLM(
                        message,
                        images.takeIf { it.isNotEmpty() } ?: emptyList())
                }
            }

            val currChatId = chatHistoryState.value.currentChatId!!
            val responseIndex = getChatMessageIndex(chatResponseMessageId)
            var isLoading = true
            flow?.collect {
                val chatMessage = chatHistoryState.value.chatMessages[responseIndex]
                chatRepository.updateChatMessage(
                    currChatId, chatResponseMessageId, if (isLoading) it.chunk
                    else chatMessage.message + it.chunk
                )
                logger.d {
                    "chunk -> ${it.chunk} :: ${it.isDone}"
                }
                if (isLoading) {
                    isLoading = false
                }
                if (it.isDone) {
                    cleanup()
                }

            }
        }
    }

    private suspend fun getChatMessageIndex(chatResponseMessageId: String): Int {
        while (chatHistoryState.value.chatMessages.indexOfFirst {
                it.id == chatResponseMessageId
            } == -1) {
            delay(50)
        }
        val responseIndex = chatHistoryState.value.chatMessages.indexOfFirst {
            it.id == chatResponseMessageId
        }
        return responseIndex
    }

    private fun isSetupAndInputValid(): Boolean {
        if (llmResponseJob?.isActive ?: false || inputFieldState.text.isEmpty()) {
            return false
        }
        if (modelManagerState.value.selectedManager == null) {
            modelManagerState.update {
                it.copy(
                    modelManagerError = ModelManagerError.InvalidRuntime
                )
            }
            return false
        }
        if (modelManagerState.value.selectedLLM == null) {
            modelManagerState.update {
                it.copy(
                    modelManagerError = ModelManagerError.InvalidModel
                )
            }
            return false
        }

        val selectedLLM = modelManagerState.value.selectedLLM!!
        if (selectedLLM.localPath == null) {
            modelManagerState.update { it.copy(ragError = "Please select the model file for ${selectedLLM.name}") }
            return false
        }
        val isRuntimeMediaSupported =
            modelManagerState.value.selectedManager?.type != ModelManagerRuntime.LlamaTIK

        // Validation: Vision
        val isVisionNeeded = documentsState.value.documents.any { it.type == MediaType.IMAGE }
        if (isVisionNeeded && selectedLLM.features.contains(ModelManagerRuntimeFeature.VISION)
                .not()
            && isRuntimeMediaSupported
        ) {
            modelManagerState.update { it.copy(ragError = "Selected model does not support Vision (images).") }
            return false
        }

        // Validation: Audio
        val isAudioNeeded = documentsState.value.documents.any { it.type == MediaType.AUDIO }
        if (isAudioNeeded && selectedLLM.features.contains(ModelManagerRuntimeFeature.AUDIO).not()
            && isRuntimeMediaSupported
        ) {
            modelManagerState.update { it.copy(ragError = "Selected model does not support Audio.") }
            return false
        }

        // Validation: RAG
        if (modelManagerState.value.selectedManager?.type != ModelManagerRuntime.LlamaTIK) {
            val isRAGNeeded = documentsState.value.documents.any { it.type == MediaType.DOCUMENT }
            if (isRAGNeeded) {
                if (modelManagerState.value.selectedEmbeddingModel == null
                    || modelManagerState.value.selectedTokenizer == null
                ) {
                    modelManagerState.update { it.copy(ragError = "Please select an Embedding Model and Tokenizer for RAG.") }
                    return false
                }
            }
            if (isRAGNeeded) {
                if (modelManagerState.value.selectedEmbeddingModel?.localPath == null) {
                    modelManagerState.update { it.copy(ragError = "Please select the file for Embedding Model") }
                    return false
                }
                if (modelManagerState.value.selectedTokenizer?.localPath == null) {
                    modelManagerState.update { it.copy(ragError = "Please select the file for Tokenizer") }
                    return false
                }
            }
        }
        return true
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

    private suspend fun addUserMessage(attachments: List<DocumentState>) {
        val messageContent = inputFieldState.text.toString()

        val chatId = chatHistoryState.value.currentChatId
        val ensureChatId = if (chatId == null) {
            val newId = chatRepository.createChat(messageContent.take(20))
            chatHistoryState.update {
                it.copy(currentChatId = newId)
            }
            newId
        } else chatId

        chatRepository.sendMessage(ensureChatId, messageContent, true, attachments)
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
                        logger.d {
                            "Permission request failed or rejected: ${e.message} :: ${e.cause}"
                        }
                    }
                }

                val file = filePicker.pickMedia()
                if (file != null && documentsState.value.documents.firstOrNull {
                        it.content == file.content || it.platformFile?.pathOrUri == file.pathOrUri
                    } == null) {
                    when (file.type) {
                        MediaType.DOCUMENT -> attachDocument(file)
                        MediaType.IMAGE -> attachDocument(file)
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to pick media" }
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
        if (ensureEmbeddingsModelLoaded()) {
            viewModelScope.launch {
                embedPendingDocuments()
            }
        }
    }

    fun onTokenizerSelected(tokenizer: TokenizerModel) {
        modelManagerState.update {
            it.copy(
                selectedTokenizer = tokenizer,
                showTokenizerSelection = false
            )
        }
        if (ensureEmbeddingsModelLoaded()) {
            viewModelScope.launch {
                embedPendingDocuments()
            }
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
            if (isLoaded.not()) {
                resetModelPath(selectedModel)
                onFilePickForModel(selectedModel)
            }
        }
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

    fun onPickEmbeddingModel(embeddingModel: EmbeddingModel) {
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

    fun onPickTokenizerModel(tokenizerModel: TokenizerModel) {
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
            content = file.content ?: "",
            isEmbedded = false,
            type = file.type,
            platformFile = file.copy(
                content = if (file.type == MediaType.DOCUMENT) null else file.content,
                bytes = if (file.type == MediaType.DOCUMENT) null else file.bytes
            )
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

    fun setChatId(chatId: String) {
        if (chatHistoryState.value.currentChatId == chatId) return
        chatHistoryState.update {
            it.copy(currentChatId = chatId)
        }
    }

    fun createNewChat() {
        if (chatHistoryState.value.currentChatId == null) {
            //Don't create a new chat if there is no chat selected already.
            //When user sends message without selected chat, new chat is created.
            return
        }
        viewModelScope.launch {
            val newChatId = chatRepository.createChat("New Chat ${Uuid.random()}")
            setChatId(newChatId)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
            val currentChatId = chatHistoryState.value.currentChatId
            if (currentChatId == chatId) {
                chatHistoryState.update {
                    it.copy(currentChatId = null)
                }
            }
        }
    }

    private suspend fun embedPendingDocuments(isReEmbedding: Boolean = false) {
        logger.d { "embedPendingDocuments: ${documentsState.value.documents.size}" }
        val pendingDocs = documentsState.value.documents.filter {
            logger.d { "embedPendingDocuments: filter-> $it" }
            if (isReEmbedding) {
                it.type == MediaType.DOCUMENT
            } else {
                it.isEmbedded.not() && it.type == MediaType.DOCUMENT && it.isSent
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
            logger.e(e) { "Failed to embed documents" }
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