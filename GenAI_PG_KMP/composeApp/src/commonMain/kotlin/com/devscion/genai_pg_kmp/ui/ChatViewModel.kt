@file:OptIn(ExperimentalUuidApi::class, ExperimentalContracts::class)

package com.devscion.genai_pg_kmp.ui

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
import com.devscion.genai_pg_kmp.domain.document.DocumentTextParser
import com.devscion.genai_pg_kmp.domain.model.ChatHistoryItem
import com.devscion.genai_pg_kmp.domain.model.ChunkedModelResponse
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
import com.devscion.genai_pg_kmp.domain.repository.VectorDBRepository
import com.devscion.genai_pg_kmp.responseformatter.FormattedResponseParser
import com.devscion.genai_pg_kmp.ui.state.ChatHistory
import com.devscion.genai_pg_kmp.ui.state.ChatUIState
import com.devscion.genai_pg_kmp.ui.state.DocumentState
import com.devscion.genai_pg_kmp.ui.state.EmbeddingProgress
import com.devscion.genai_pg_kmp.ui.state.ModelManagerError
import com.devscion.genai_pg_kmp.ui.state.ModelManagerState
import com.devscion.genai_pg_kmp.ui.state.RAGDocumentsState
import com.devscion.genai_pg_kmp.utils.Constants.CHAT_ID_METADATA_KEY
import com.devscion.genai_pg_kmp.utils.Constants.TITLE_METADATA_KEY
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.storage.STORAGE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
    private val chatRepository: ChatRepository,
    private val documentTextParser: DocumentTextParser,
    private val vectorDBRepository: VectorDBRepository,
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
//                      TODO: need to show document names for selected chat in some kind of list design on UI
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

//    private fun parseAndSetDocuments() {
//        val docs = mutableListOf<DocumentState>()
//        chatHistoryState.value.chatMessages.forEach {
//            if (it.attachments.isNotEmpty()) {
//                logger.d {
//                    "parseAndSetDocs: ${it.attachments}"
//                }
//            }
//            docs.addAll(it.attachments)
//        }
//        documentsState.update {
//            it.copy(
//                documents = docs,
//                isEmbedding = false,
//            )
//        }
//    }

    private fun loadModelManagerOptions() = when (platformDetailProvider.getPlatform()) {
        Platform.ANDROID -> androidOptions()
        Platform.IOS -> iOSOptions()
    }

    fun onSend() {
        if (isSetupAndInputValid().not()) return
        val message = inputFieldState.text.toString()
        val attachments = documentsState.value.documents.toList()

        llmResponseJob = viewModelScope.launch {
            val activeJob = coroutineContext[Job]
            modelManagerState.update { it.copy(isGeneratingResponse = true) }

            try {
                val chatId = addUserMessage(message, attachments)
                resetInputField()

                val responseMessage = withContext(ioDispatcher) {
                    chatRepository.sendMessage(
                        chatId = chatId,
                        content = responsePlaceHolderText,
                        isFromUser = false,
                        attachments = emptyList(),
                    )
                }
                addVisibleMessage(chatId, responseMessage)

                documentsState.update { state ->
                    state.copy(
                        documents = state.documents.map { it.copy(isSent = true) }
                    )
                }

                val currentDocuments = documentsState.value.documents.toList()
                val ragDocuments = currentDocuments.filter { it.type == MediaType.DOCUMENT }
                val images = currentDocuments
                    .filter { it.type != MediaType.DOCUMENT }
                    .mapNotNull { it.platformFile }

                val responseFlow = createResponseFlow(
                    message = message,
                    ragDocuments = ragDocuments,
                    images = images
                ) ?: return@launch

                collectModelResponse(
                    chatId = chatId,
                    responseMessageId = responseMessage.id,
                    flow = responseFlow
                )
            } catch (cancelled: CancellationException) {
                logger.d { "onSend cancelled" }
                throw cancelled
            } catch (e: Exception) {
                logger.e(e) { "Failed to handle model response" }
            } finally {
                if (llmResponseJob == activeJob) {
                    llmResponseJob = null
                }
                modelManagerState.update {
                    it.copy(
                        isGeneratingResponse = false
                    )
                }
            }
        }
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

    private fun cancelActiveResponse() {
        val activeJob = llmResponseJob ?: return
        llmResponseJob = null
        activeJob.cancel()
        modelManagerState.update {
            it.copy(
                isGeneratingResponse = false
            )
        }
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                modelManager?.stopResponseGeneration()
            }.onFailure { error ->
                logger.e(error) { "Failed to stop response generation" }
            }
        }
    }

    fun stopGeneratingResponse() {
        viewModelScope.launch {
            cancelActiveResponseAndWait()
        }
    }

    private suspend fun cancelActiveResponseAndWait() {
        val activeJob = llmResponseJob ?: return
        llmResponseJob = null
        activeJob.cancel()
        modelManagerState.update {
            it.copy(
                isGeneratingResponse = false
            )
        }
        runCatching {
            withContext(ioDispatcher) {
                modelManager?.stopResponseGeneration()
            }
        }.onFailure { error ->
            logger.e(error) { "Failed to stop response generation" }
        }
        runCatching { activeJob.join() }
    }

    private fun resetInputField() {
        inputFieldState.edit {
            this.replace(0, originalText.length, "")
        }
    }

    private suspend fun addUserMessage(
        messageContent: String,
        attachments: List<DocumentState>
    ): String {
        val chatId = chatHistoryState.value.currentChatId
        val ensureChatId = if (chatId == null) {
            val newId = withContext(ioDispatcher) {
                chatRepository.createChat(messageContent.take(20))
            }
            chatHistoryState.update {
                it.copy(currentChatId = newId)
            }
            newId
        } else chatId

        withContext(ioDispatcher) {
            chatRepository.sendMessage(ensureChatId, messageContent, true, attachments)
        }.let {
            addVisibleMessage(ensureChatId, it)
        }
        return ensureChatId
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
                        MediaType.DOCUMENT -> attachDocument(
                            parseDocumentAttachment(file) ?: return@launch
                        )

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
        cancelActiveResponse()
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

    private suspend fun parseDocumentAttachment(file: PlatformFile): PlatformFile? {
        val parsedContent = documentTextParser.parse(file)
        if (parsedContent.isNullOrBlank()) {
            modelManagerState.update {
                it.copy(
                    ragError = "Couldn't extract readable text from ${file.name}. Try PDF, CSV, TSV, TXT, Markdown, JSON, XML, HTML, YAML, or log files."
                )
            }
            return null
        }
        modelManagerState.update { it.copy(ragError = null) }
        return file.copy(content = parsedContent)
    }

    fun removeDocument(documentId: String) {
        val document = documentsState.value.documents.firstOrNull { it.id == documentId }
        documentsState.update {
            it.copy(documents = it.documents.filter { doc -> doc.id != documentId })
        }
        document?.takeIf { it.isEmbedded && it.type == MediaType.DOCUMENT }?.let {
//           TODO: remove the embedding of this chat's removed document in db
            //            viewModelScope.launch {
//                embedPendingDocuments(isReEmbedding = true)
//            }
        }
    }

    fun setChatId(chatId: String) {
        if (chatHistoryState.value.currentChatId == chatId) return
        cancelActiveResponse()
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
            cancelActiveResponseAndWait()
            val newChatId = withContext(ioDispatcher) {
                chatRepository.createChat("New Chat ${Uuid.random()}")
            }
            setChatId(newChatId)
            modelManager?.clearIndexedDocuments()
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val currentChatId = chatHistoryState.value.currentChatId
            if (currentChatId == chatId) {
                cancelActiveResponseAndWait()
                chatHistoryState.update {
                    it.copy(currentChatId = null)
                }
            }
            withContext(ioDispatcher) {
                chatRepository.deleteChat(chatId)
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
            pendingDocs.forEachIndexed { index, doc ->
                val ragDocument = RAGDocument(
                    id = doc.id,
                    content = doc.content,
                    metadata = mapOf(
                        TITLE_METADATA_KEY to doc.title,
                        CHAT_ID_METADATA_KEY to chatHistoryState.value.currentChatId!!,
                    )
                )

                if (vectorDBRepository.hasChatDocumentEmbeddings(
                        chatHistoryState.value.currentChatId!!,
                        doc.title,
                    ).not()
                ) {
                    modelManager!!.indexDocument(ragDocument)
                }

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

    private suspend fun setupRAGModels() {
        modelManagerState.value.let { state ->
            modelManager!!.loadEmbeddingModel(
                state.selectedEmbeddingModel!!.localPath!!,
                state.selectedTokenizer!!.localPath!!,
            )
        }
    }

    private suspend fun createResponseFlow(
        message: String,
        ragDocuments: List<DocumentState>,
        images: List<PlatformFile>
    ): Flow<ChunkedModelResponse>? {
        val manager = modelManager ?: return null
        val chatId = chatHistoryState.value.currentChatId ?: return null

        setupRAGModels()
        if (ragDocuments.isNotEmpty()) {
            embedPendingDocuments()
        }
        resetDocumentsState()
        return manager.sendPromptWithRAG(message, chatId = chatId, images = images)
    }

    private suspend fun collectModelResponse(
        chatId: String,
        responseMessageId: String,
        flow: Flow<ChunkedModelResponse>
    ) {
        val responseBuilder = StringBuilder()
        var latestResponseText: String? = null
        var persistedResponseText: String? = null
        val persistenceChannel = Channel<String>(capacity = Channel.CONFLATED)
        val dbWriterJob = viewModelScope.launch(ioDispatcher) {
            for (content in persistenceChannel) {
                if (content == persistedResponseText) continue
                chatRepository.updateChatMessage(
                    chatId = chatId,
                    messageId = responseMessageId,
                    content = content
                )
                persistedResponseText = content
            }
        }

        try {
            flow.collect { response ->
                if (response.chunk.isNotEmpty()) {
                    responseBuilder.append(response.chunk)
                }

                val fullResponse = FormattedResponseParser.normalize(responseBuilder.toString())
                if (fullResponse == latestResponseText) {
                    return@collect
                }

                updateVisibleMessage(chatId, responseMessageId, fullResponse)
                latestResponseText = fullResponse
                persistenceChannel.trySend(fullResponse)
            }
        } finally {
            if (latestResponseText == null) {
                updateVisibleMessage(chatId, responseMessageId, "")
                latestResponseText = ""
                persistenceChannel.trySend("")
            }

            persistenceChannel.close()
            withContext(NonCancellable) {
                dbWriterJob.join()
            }

            if (persistedResponseText != latestResponseText) {
                withContext(NonCancellable + ioDispatcher) {
                    chatRepository.updateChatMessage(
                        chatId = chatId,
                        messageId = responseMessageId,
                        content = latestResponseText.orEmpty()
                    )
                }
                persistedResponseText = latestResponseText
            }
        }
    }

    private fun addVisibleMessage(chatId: String, message: ChatHistoryItem) {
        if (chatHistoryState.value.currentChatId != chatId) return
        chatHistoryState.value.chatMessages.add(message)
    }

    private fun updateVisibleMessage(chatId: String, messageId: String, content: String) {
        if (chatHistoryState.value.currentChatId != chatId) return
        val index = chatHistoryState.value.chatMessages.indexOfFirst { it.id == messageId }
        if (index == -1) return
        val currentMessage = chatHistoryState.value.chatMessages[index]
        if (currentMessage.message == content) return
        chatHistoryState.value.chatMessages[index] = currentMessage.copy(message = content)
    }

}
