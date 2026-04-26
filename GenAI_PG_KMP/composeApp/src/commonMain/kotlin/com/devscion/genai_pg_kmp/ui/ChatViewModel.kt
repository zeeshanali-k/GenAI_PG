@file:OptIn(ExperimentalUuidApi::class, ExperimentalContracts::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.LLMRuntimeManager
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import kotlin.contracts.ExperimentalContracts
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val platformDetailProvider: PlatformDetailProvider,
    private val filePicker: FilePicker,
    private val modelPathProvider: ModelPathProvider,
    private val modelSettings: ModelSettings,
    private val chatRepository: ChatRepository
) : ViewModel(), KoinComponent {

    private val ioDispatcher = Dispatchers.IO + SupervisorJob()

    private val responsePlaceHolderText = "Generating..."
    private val logger = Logger.withTag("ChatVM")

    val lazyListState = LazyListState()

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

    private var modelManager: LLMRuntimeManager? = null

    init {
        viewModelScope.launch {
            var lastChatId = chatHistoryState.value.currentChatId
            chatHistoryState.collectLatest { state ->
                val chatId = state.currentChatId
                logger.withTag("ChatHistoryState").d {
                    "currentChatId: $chatId :: $lastChatId"
                }
                if (chatId != null && chatId != lastChatId) {
                    chatRepository.getMessagesList(chatId)
                        .let { messages ->
                            chatHistoryState.value.chatMessages.clear()
                            chatHistoryState.value.chatMessages.addAll(messages)
                            logger.d { "collectLatest: ${messages.size} :: $lastChatId - $chatId" }
//                      TODO: need to show document names in some kind of list design on UI
//                       if (lastChatId != chatId && messages.isNotEmpty()) {
//                                parseAndSetDocuments()
//                                resetDocumentsState()
//                            }
                            lastChatId = chatId
                        }
                } else {
                    chatHistoryState.value.chatMessages.clear()
                    chatHistoryState.value.chatMessages.addAll(emptyList())
                }
            }
        }
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
        llmResponseJob = viewModelScope.launch(ioDispatcher) {
            addUserMessage(documentsState.value.documents.toList())
            val message = inputFieldState.text.toString()
            resetInputField()
            val chatResponseMessageId = chatRepository.sendMessage(
                chatId = chatHistoryState.value.currentChatId!!,
                content = responsePlaceHolderText,
                isFromUser = false,
                attachments = emptyList(),
            ).let {
                chatHistoryState.value.chatMessages.add(it)
                it.id
            }
            logger.d { "onSend: messages count-> ${chatHistoryState.value.chatMessages.size}" }
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
            val ragDocuments =
                documentsState.value.documents.filter { it.type == MediaType.DOCUMENT }
            // Filter attachments for Multimodal (Image, Audio)
            val images = documentsState.value.documents
                .filter { it.type != MediaType.DOCUMENT }
                .mapNotNull { it.platformFile }

            val flow = (if (ragDocuments.isNotEmpty()) {
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
            })?.stateIn(viewModelScope, SharingStarted.Eagerly, null)

            val currChatId = chatHistoryState.value.currentChatId!!
            val responseIndex = getChatResponseMessageIndex(chatResponseMessageId)
            var isLoading = true
            launch {
                flow?.collect {
                    if (it == null) return@collect
                    val chatMessage = chatHistoryState.value.chatMessages[responseIndex]
//                logger.d {
//                    "collect-> Model Response-> ${it.chunk} :: $chatMessage :: $isLoading"
//                }
                    chatHistoryState.value.chatMessages[responseIndex] = chatMessage.copy(
                        message = if (isLoading || chatMessage.message.contains(
                                responsePlaceHolderText,
                                ignoreCase = true
                            )
                        ) it.chunk
                        else chatMessage.message + it.chunk
                    )
//                    logger.d {
//                        "chunk -> ${it.chunk} :: ${it.isDone}"
//                    }
                    if (isLoading) {
                        isLoading = false
                    }
                    if (it.isDone) {
                        cleanup()
                    }
                }
            }
            launch {
                flow?.collect {
                    if (it == null) return@collect
                    val chatMessage = chatHistoryState.value.chatMessages[responseIndex]
                    logger.d { "DB Message Saving-> ${it.chunk}" }
                    chatRepository.updateChatMessage(
                        chatId = currChatId,
                        messageId = chatMessage.id,
                        content = if (isLoading || chatMessage.message.contains(
                                responsePlaceHolderText,
                                ignoreCase = true
                            )
                        ) it.chunk
                        else chatMessage.message + it.chunk
                    )
                    lazyListState.scrollBy(Float.MAX_VALUE)
                    logger.d { "DB Message Saved and Scrolled" }
                }
            }
        }
    }

    private suspend fun getChatResponseMessageIndex(chatResponseMessageId: String): Int {
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
        val isMediaSupported =
            modelManagerState.value.selectedManager?.type != ModelManagerRuntime.LlamaTIK

        if (isMediaSupported) {
            // Validation: Vision
            val isVisionNeeded = documentsState.value.documents.any { it.type == MediaType.IMAGE }
            if (isVisionNeeded && selectedLLM.features.contains(ModelManagerRuntimeFeature.VISION)
                    .not()
            ) {
                modelManagerState.update { it.copy(ragError = "Selected model does not support Vision (images).") }
                return false
            }

            // Validation: Audio
            val isAudioNeeded = documentsState.value.documents.any { it.type == MediaType.AUDIO }
            if (isAudioNeeded && selectedLLM.features.contains(ModelManagerRuntimeFeature.AUDIO)
                    .not()
            ) {
                modelManagerState.update { it.copy(ragError = "Selected model does not support Audio.") }
                return false
            }
        }
        return if (platformDetailProvider.getPlatform() == Platform.IOS) {
            true
        } else {
            validateRagSetup()
        }
    }

    private fun validateRagSetup(): Boolean {
        // Validation: RAG
        val isRAGNeeded = documentsState.value.documents.any { it.type == MediaType.DOCUMENT }
        if (isRAGNeeded) {
            if (modelManagerState.value.selectedEmbeddingModel == null
            ) {
                modelManagerState.update { it.copy(ragError = "Please select an Embedding Model for RAG.") }
                return false
            }

            if (modelManagerState.value.selectedManager?.type != ModelManagerRuntime.LlamaTIK) {
                if (modelManagerState.value.selectedTokenizer == null) {
                    modelManagerState.update { it.copy(ragError = "Please select a Tokenizer for RAG.") }
                    return false
                }
            }
        }
        if (isRAGNeeded) {
            if (modelManagerState.value.selectedEmbeddingModel?.localPath == null) {
                modelManagerState.update { it.copy(ragError = "Please select the file for Embedding Model") }
                return false
            }
            if (modelManagerState.value.selectedTokenizer?.localPath == null
                && modelManagerState.value.selectedManager?.type != ModelManagerRuntime.LlamaTIK
            ) {
                modelManagerState.update { it.copy(ragError = "Please select the file for Tokenizer") }
                return false
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

        chatRepository.sendMessage(ensureChatId, messageContent, true, attachments).let {
            chatHistoryState.value.chatMessages.add(it)
        }
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
                            "Permission request failed or rejected: $e ${e.message} :: ${e.cause}"
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
        if (model.localPath.isNullOrEmpty()) {
            onPickEmbeddingModel(model)
            return
        }
        modelManagerState.update {
            it.copy(
                selectedEmbeddingModel = model,
                showEmbeddingSelection = false
            )
        }
    }

    fun onTokenizerSelected(tokenizer: TokenizerModel) {
        if (tokenizer.localPath.isNullOrEmpty()) {
            onPickTokenizerModel(tokenizer)
            return
        }
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
                filePicker.pickFile(modelManagerState.value.selectedManager!!.supportedModelFormats.map { it.format })
            if (file != null) {
                modelManagerState.update { state ->
                    val updatedModel = model.copy(localPath = file.pathOrUri)
                    updatedModel.let { modelSettings.savePath(id = it.id, path = file.pathOrUri) }
                    state.copy(
                        llmList = state.llmList?.map {
                            if (it.id == updatedModel.id) updatedModel else it
                        },
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
        viewModelScope.launch(Dispatchers.IO) {
            val selectedModel = modelManagerState.value.selectedLLM!!

            val path = selectedModel.localPath?.let {
                modelPathProvider.resolvePath(it)
            }
            val isLoaded = modelManager!!.loadModel(
                selectedModel.copy(
                    localPath = path,
                )
            )
            withContext(Dispatchers.Main) {
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
            val file =
                filePicker.pickFile(modelManagerState.value.selectedManager!!.supportedRagModelFormats.map { it.format })
            logger.d { "onPickEmbeddingModel-> $file \n\n\t\t ${modelManagerState.value.selectedEmbeddingModel}" }
            if (file != null) {
                modelManagerState.update { state ->
                    val updatedModel =
                        embeddingModel.copy(localPath = file.pathOrUri)
                    updatedModel.let { modelSettings.savePath(it.id, it.localPath) }
                    state.copy(
                        embeddingModels = state.embeddingModels.map {
                            if (it.id == updatedModel.id) updatedModel else it
                        },
                        selectedEmbeddingModel = updatedModel
                    )
                }
            }
        }
    }

    fun onPickTokenizerModel(tokenizerModel: TokenizerModel) {
        viewModelScope.launch {
            val file =
                filePicker.pickFile(modelManagerState.value.selectedManager!!.supportedRagModelFormats.map { it.format })
            if (file != null) {
                modelManagerState.update { state ->
                    val updatedTokenizer = tokenizerModel.copy(localPath = file.pathOrUri)
                    updatedTokenizer.let { modelSettings.savePath(it.id, it.localPath) }
                    state.copy(
                        tokenizerModels = state.tokenizerModels.map {
                            if (it.id == updatedTokenizer.id) updatedTokenizer else it
                        },
                        selectedTokenizer = updatedTokenizer
                    )
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
        val features = modelManagerState.value.selectedManager?.features
        if ((features?.contains(ModelManagerRuntimeFeature.VISION)?.not() ?: false)
            && features.contains(ModelManagerRuntimeFeature.AUDIO).not()
            && file.type in listOf(MediaType.AUDIO, MediaType.IMAGE)
        ) {
            modelManagerState.update {
                it.copy(
                    ragError = "Selected runtime doesn't support media inference yet."
                )
            }
            return
        }
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
            modelManager?.clearIndexedDocuments()
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
        println("ChatVM: embedPendingDocuments-> ${documentsState.value.documents.size}")
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
                    metadata = mapOf(
                        "title" to doc.title,
                        "chat_id" to chatHistoryState.value.currentChatId!!,
                    )
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
