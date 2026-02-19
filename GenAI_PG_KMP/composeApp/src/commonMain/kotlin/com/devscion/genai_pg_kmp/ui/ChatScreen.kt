@file:OptIn(ExperimentalMaterial3Api::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devscion.genai_pg_kmp.LocalAnimatedVisibilityScope
import com.devscion.genai_pg_kmp.LocalTransitionScope
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.model.EmbeddingModel
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.domain.model.ModelManagerRuntime
import com.devscion.genai_pg_kmp.domain.model.TokenizerModel
import com.devscion.genai_pg_kmp.ui.components.AttachedDocumentChip
import com.devscion.genai_pg_kmp.ui.components.ChatBubble
import com.devscion.genai_pg_kmp.ui.components.ChatDrawerContent
import com.devscion.genai_pg_kmp.ui.components.ChatInput
import com.devscion.genai_pg_kmp.ui.components.SelectionButton
import com.devscion.genai_pg_kmp.ui.dialogs.ErrorMessageDialog
import com.devscion.genai_pg_kmp.ui.dialogs.OptionSelectionContent
import com.devscion.genai_pg_kmp.ui.state.ChatHistory
import com.devscion.genai_pg_kmp.ui.state.ChatUIState
import com.devscion.genai_pg_kmp.ui.state.ModelManagerError
import com.devscion.genai_pg_kmp.ui.state.ModelManagerState
import com.devscion.genai_pg_kmp.ui.state.RAGDocumentsState
import com.devscion.genai_pg_kmp.utils.plainClickable
import com.devscion.genai_pg_kmp.utils.toComposeImageBitmap
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import genai_pg.genai_pg_kmp.composeapp.generated.resources.Res
import genai_pg.genai_pg_kmp.composeapp.generated.resources.app_name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val viewModel = koinViewModel<ChatViewModel>()

    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }

    LaunchedEffect(controller) {
        viewModel.setPermissionsController(controller)
    }

    BindEffect(controller)

    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatDrawerContent(
                    chats = (uiState as? ChatUIState.Success)?.chatHistory?.chats ?: emptyList(),
                    currentChatId = (uiState as? ChatUIState.Success)?.chatHistory?.currentChatId,
                    onChatSelected = { chatId ->
                        viewModel.setChatId(chatId)
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.createNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteChatClick = { chatId ->
                        viewModel.deleteChat(chatId)
                    }
                )
            }
        }
    ) {
        val focusManager = LocalFocusManager.current

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(stringResource(Res.string.app_name))
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            modifier = Modifier
                .plainClickable {
                    focusManager.clearFocus()
                }
                .fillMaxSize()
                .navigationBarsPadding(),
        ) { paddingValues ->

            (uiState as? ChatUIState.Success)?.let {
                ChatHistoryContent(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    inputFieldState = viewModel.inputFieldState,
                    onSendClick = {
                        if (viewModel.inputFieldState.text.isNotEmpty()) {
                            focusManager.clearFocus(force = true)
                        }
                        viewModel.onSend()
                    },
                    onAttachMediaClick = viewModel::onAttachMedia,
                    toggleManagerSelection = viewModel::toggleManagerSelection,
                    onToggleModelSelection = viewModel::toggleModelSelection,
                    onToggleEmbeddingSelection = viewModel::toggleEmbeddingSelection,
                    onToggleTokenizerSelection = viewModel::toggleTokenizerSelection,
                    onStopClick = viewModel::stopGeneratingResponse,
                    onToggleRuntimeSelection = viewModel::toggleManagerSelection,
                    onRuntimeSelected = viewModel::onRuntimeSelected,
                    onModelSelected = viewModel::onModelSelected,
                    onEmbeddingSelected = viewModel::onEmbeddingModelSelected,
                    onTokenizerSelected = viewModel::onTokenizerSelected,
                    onFilePickForEmbedding = viewModel::onPickEmbeddingModel,
                    onFilePickForTokenizer = viewModel::onPickTokenizerModel,
                    modelManagerState = it.modelManagerState,
                    chatHistory = it.chatHistory,
                    documentsState = it.documentsState,
                    onRemoveDocument = viewModel::removeDocument,
                    onFilePickForModel = viewModel::onFilePickForModel,
                )
            }


            (uiState as? ChatUIState.Success)?.ChatScreenDialogs(
                onResetError = viewModel::resetError,
                onResetRagError = viewModel::resetRagError
            )
        }
    }
}


@Composable
fun ChatHistoryContent(
    modifier: Modifier,
    modelManagerState: ModelManagerState,
    chatHistory: ChatHistory,
    inputFieldState: TextFieldState,
    onToggleModelSelection: () -> Unit,
    onToggleEmbeddingSelection: () -> Unit,
    onToggleTokenizerSelection: () -> Unit,
    onAttachMediaClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    toggleManagerSelection: () -> Unit,
    onToggleRuntimeSelection: () -> Unit,
    onModelSelected: (Model) -> Unit,
    onRuntimeSelected: (ModelManagerOption) -> Unit,
    onEmbeddingSelected: (EmbeddingModel) -> Unit,
    onTokenizerSelected: (TokenizerModel) -> Unit,
    onFilePickForEmbedding: (EmbeddingModel) -> Unit,
    onFilePickForTokenizer: (TokenizerModel) -> Unit,
    onFilePickForModel: (Model) -> Unit,
    documentsState: RAGDocumentsState,
    onRemoveDocument: (String) -> Unit,
) {
    val isRAGEnabled = documentsState.documents.any { it.type == MediaType.DOCUMENT }

    var showEmbeddingModelOptions by rememberSaveable {
        mutableStateOf(false)
    }
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalTransitionScope provides this) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Runtime Selection Button
                        AnimatedContent(
                            modifier = Modifier.weight(1f),
                            targetState = modelManagerState.showManagerSelection,
                            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                        ) { showManagerSelection ->
                            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                SelectionButton(
                                    modifier = with(LocalTransitionScope.current!!) {
                                        Modifier.fillMaxWidth()
                                            .then(
                                                if (showManagerSelection.not())
                                                    Modifier.sharedBounds(
                                                        rememberSharedContentState("optionSelectionContent1"),
                                                        LocalAnimatedVisibilityScope.current!!,
                                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                                    )
                                                else Modifier
                                            )
                                    },
                                    title = modelManagerState.selectedManager?.managerName
                                        ?: "Select Runtime",
                                    isSelected = false,
                                    onClick = toggleManagerSelection,
                                )
                            }
                        }

                        // Model Selection Button
                        if (modelManagerState.selectedManager != null) {
                            AnimatedContent(
                                modifier = Modifier.weight(1f),
                                targetState = modelManagerState.showModelSelection,
                                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                            ) { showModelSelection ->
                                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                    SelectionButton(
                                        modifier = with(LocalTransitionScope.current!!) {
                                            Modifier.fillMaxWidth()
                                                .then(
                                                    if (showModelSelection.not())
                                                        Modifier.sharedBounds(
                                                            rememberSharedContentState("optionSelectionContent2"),
                                                            LocalAnimatedVisibilityScope.current!!,
                                                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                                        )
                                                    else Modifier
                                                )
                                        },
                                        title = if (modelManagerState.selectedLLM == null && modelManagerState.llmList.isNullOrEmpty()) "Unavailable"
                                        else modelManagerState.selectedLLM?.name
                                            ?: "Select Model",
                                        isSelected = false,
                                        onClick = onToggleModelSelection,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        AnimatedContent(
                            modelManagerState.selectedManager != null &&
                                    modelManagerState.selectedLLM != null
                                    && modelManagerState.selectedManager.type != ModelManagerRuntime.LlamaTIK
                        ) {
                            if (it) {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    "",
                                    modifier = Modifier.size(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        )
                                        .rotate(if (showEmbeddingModelOptions) 180f else 0f)
                                        .plainClickable {
                                            showEmbeddingModelOptions =
                                                showEmbeddingModelOptions.not()
                                        }
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        modelManagerState.selectedLLM != null
                                && (isRAGEnabled || showEmbeddingModelOptions)
                                && modelManagerState.selectedManager?.type != ModelManagerRuntime.LlamaTIK,
                        transitionSpec = {
                            slideInVertically { -it }.togetherWith(slideOutVertically { it })
                        }
                    ) {
                        if (it) {
                            Row(
                                modifier = Modifier
                                    .animateContentSize(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Embedding Selection Button
                                AnimatedContent(
                                    modifier = Modifier.weight(1f),
                                    targetState = modelManagerState.showEmbeddingSelection,
                                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                                ) { showEmbeddingSelection ->
                                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                        SelectionButton(
                                            modifier = with(LocalTransitionScope.current!!) {
                                                Modifier.fillMaxWidth()
                                                    .then(
                                                        if (showEmbeddingSelection.not())
                                                            Modifier.sharedBounds(
                                                                rememberSharedContentState("optionSelectionContent3"),
                                                                LocalAnimatedVisibilityScope.current!!,
                                                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                                            )
                                                        else Modifier
                                                    )
                                            },
                                            title = modelManagerState.selectedEmbeddingModel?.name
                                                ?: "Select Embedding",
                                            isSelected = false,
                                            onClick = onToggleEmbeddingSelection,
                                        )
                                    }
                                }

                                // Tokenizer Selection Button
                                AnimatedContent(
                                    modifier = Modifier.weight(1f),
                                    targetState = modelManagerState.showTokenizerSelection,
                                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                                ) { showTokenizerSelection ->
                                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                        SelectionButton(
                                            modifier = with(LocalTransitionScope.current!!) {
                                                Modifier.fillMaxWidth()
                                                    .then(
                                                        if (showTokenizerSelection.not())
                                                            Modifier.sharedBounds(
                                                                rememberSharedContentState("optionSelectionContent4"),
                                                                LocalAnimatedVisibilityScope.current!!,
                                                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                                            )
                                                        else Modifier
                                                    )
                                            },
                                            title = modelManagerState.selectedTokenizer?.name
                                                ?: "Select Tokenizer",
                                            isSelected = false,
                                            onClick = onToggleTokenizerSelection,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val listState = rememberLazyListState()
                    LaunchedEffect(chatHistory.chatMessages) {
                        listState.scrollBy(Float.MAX_VALUE)
                    }
                    LazyColumn(
                        Modifier.fillMaxWidth()
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.shapes.medium
                            ),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(
                            vertical = 12.dp
                        )
                    ) {
                        itemsIndexed(
                            chatHistory.chatMessages,
                            key = { _, i -> i.id }) { index, item ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (item.isLLMResponse) Alignment.Start
                                else Alignment.End,
                            ) {
                                ChatBubble(
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                    index = index,
                                    isSent = item.isLLMResponse.not()
                                ) {
                                    Column(
                                        Modifier
                                            .padding(12.dp),
                                        horizontalAlignment = if (item.isLLMResponse) Alignment.Start
                                        else Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item.attachments.forEach { doc ->
                                            if (doc.type == MediaType.IMAGE && doc.platformFile?.bytes != null) {
                                                val bitmap =
                                                    doc.platformFile.bytes.toComposeImageBitmap()
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = "Attached Image",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 200.dp)
                                                        .clip(MaterialTheme.shapes.medium),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                AttachedDocumentChip(
                                                    document = doc,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                        if (item.message.isNotEmpty()) {
                                            Text(item.message)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ChatInput(
                        state = inputFieldState,
                        documentsState = documentsState,
                        isGeneratingResponse = modelManagerState.isGeneratingResponse,
                        onAttachMediaClick = onAttachMediaClick,
                        onRemoveDocument = onRemoveDocument,
                        onSendClick = onSendClick,
                        onStopClick = onStopClick,
                    )
                }

                Column(
                    Modifier
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val scope = rememberCoroutineScope()
                    Row {
                        // Manager selection dialog
                        AnimatedContent(
                            targetState = modelManagerState.showManagerSelection,
                            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                        ) { showManagerSelection ->
                            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                if (showManagerSelection) {
                                    OptionSelectionContent(
                                        modifier = with(LocalTransitionScope.current!!) {
                                            Modifier
                                                .weight(1f)
                                                .sharedBounds(
                                                    rememberSharedContentState("optionSelectionContent1"),
                                                    LocalAnimatedVisibilityScope.current!!,
                                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                )
                                        },
                                        title = "Select Runtime",
                                        options = modelManagerState.modelManagerOptions,
                                        selectedOption = modelManagerState.selectedManager,
                                        getName = { managerName },
                                        getTags = { features.map { it.title } },
                                        getFormats = { supportedFormats.map { it.format } },
                                        getDescription = { desciption },
                                        getDownloadUrl = { null },
                                        getLocalPath = { null },
                                        onDismiss = onToggleRuntimeSelection,
                                        onSelect = {
                                            onRuntimeSelected(this)
                                            onToggleRuntimeSelection()
                                            scope.launch(Dispatchers.Main.immediate) {
                                                delay(250)
                                                onToggleModelSelection()
                                            }
                                        },
                                        onFileSelect = {},
                                        showStatus = false,
                                        showDownload = false,
                                        showFileSelect = false,
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        AnimatedContent(
                            targetState = modelManagerState.showModelSelection && modelManagerState.llmList.isNullOrEmpty()
                                .not(),
                            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                        ) { showModelSelection ->
                            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                if (showModelSelection) {
                                    OptionSelectionContent(
                                        modifier = with(LocalTransitionScope.current!!) {
                                            Modifier
                                                .weight(1f)
                                                .sharedBounds(
                                                    rememberSharedContentState("optionSelectionContent2"),
                                                    LocalAnimatedVisibilityScope.current!!,
                                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                )
                                        },
                                        title = "Select Model",
                                        options = modelManagerState.llmList ?: emptyList(),
                                        selectedOption = modelManagerState.selectedLLM,
                                        getName = { name },
                                        getDescription = { description },
                                        getDownloadUrl = { downloadUrl },
                                        getLocalPath = { localPath },
                                        onDismiss = onToggleModelSelection,
                                        onSelect = {
                                            onModelSelected(this)
                                            onToggleModelSelection()
                                        },
                                        onFileSelect = {
                                            onFilePickForModel(this)
                                        },
                                        maxListHeight = 500.dp,
                                        getTags = { features.map { it.title } }
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (modelManagerState.showEmbeddingSelection || modelManagerState.showTokenizerSelection) {
                        Row {
                            AnimatedContent(
                                targetState = modelManagerState.showEmbeddingSelection,
                                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                            ) { showEmbeddingSelection ->
                                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                    if (showEmbeddingSelection) {
                                        OptionSelectionContent(
                                            modifier = with(LocalTransitionScope.current!!) {
                                                Modifier
                                                    .weight(1f)
                                                    .sharedBounds(
                                                        rememberSharedContentState("optionSelectionContent3"),
                                                        LocalAnimatedVisibilityScope.current!!,
                                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                    )
                                            },
                                            title = "Select Embedding Model",
                                            options = modelManagerState.embeddingModels,
                                            selectedOption = modelManagerState.selectedEmbeddingModel,
                                            getName = { name },
                                            getDescription = { description },
                                            getDownloadUrl = { downloadUrl },
                                            getLocalPath = { localPath },
                                            onDismiss = onToggleEmbeddingSelection,
                                            onSelect = { onEmbeddingSelected(this) },
                                            onFileSelect = onFilePickForEmbedding,
                                            getTags = { emptyList() }
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }

                            AnimatedContent(
                                targetState = modelManagerState.showTokenizerSelection,
                                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
                            ) { showTokenizerSelection ->
                                CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                    if (showTokenizerSelection) {
                                        OptionSelectionContent(
                                            modifier = with(LocalTransitionScope.current!!) {
                                                Modifier
                                                    .weight(1f)
                                                    .sharedBounds(
                                                        rememberSharedContentState("optionSelectionContent4"),
                                                        LocalAnimatedVisibilityScope.current!!,
                                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                    )
                                            },
                                            title = "Select Tokenizer",
                                            options = modelManagerState.tokenizerModels,
                                            selectedOption = modelManagerState.selectedTokenizer,
                                            getName = { name },
                                            getDescription = { description },
                                            getDownloadUrl = { downloadUrl },
                                            getLocalPath = { localPath },
                                            onDismiss = onToggleTokenizerSelection,
                                            onSelect = { onTokenizerSelected(this) },
                                            onFileSelect = onFilePickForTokenizer,
                                            getTags = { emptyList() }
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatUIState.Success.ChatScreenDialogs(
    onResetError: () -> Unit,
    onResetRagError: () -> Unit,
) {
    if (modelManagerState.isLoadingModel) {
        Dialog(onDismissRequest = {}) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
    var title by rememberSaveable() {
        mutableStateOf<String?>(null)
    }
    var message by rememberSaveable() {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(modelManagerState.modelManagerError) {
        when (modelManagerState.modelManagerError) {
            ModelManagerError.FailedToLoadModel -> {
                title = "Failed to load model"
                message = "Verify if you have placed the model in correct directory"
            }

            ModelManagerError.Initial -> {
                title = null
                message = null
            }

            ModelManagerError.InvalidModel -> {
                title = "Invalid Model Selected"
                message = "You must select a valid model before starting the chat"
            }

            ModelManagerError.InvalidRuntime -> {
                title = "Invalid Runtime"
                message = "You must select a valid Runtime before starting the chat"
            }
        }
    }

    if (title.isNullOrEmpty().not() && message.isNullOrEmpty().not()) {
        ErrorMessageDialog(
            title = title!!,
            message = message!!,
            onDismiss = onResetError,
        )
    }

    if (modelManagerState.ragError != null) {
        ErrorMessageDialog(
            title = "Incomplete Setup",
            message = modelManagerState.ragError,
            onDismiss = onResetRagError
        )
    }
}
