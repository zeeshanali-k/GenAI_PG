@file:OptIn(ExperimentalMaterial3Api::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devscion.genai_pg_kmp.LocalAnimatedVisibilityScope
import com.devscion.genai_pg_kmp.LocalTransitionScope
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.ui.components.ChatBubble
import com.devscion.genai_pg_kmp.ui.components.ChatInput
import com.devscion.genai_pg_kmp.ui.components.SelectionButton
import com.devscion.genai_pg_kmp.ui.dialogs.ErrorMessageDialog
import com.devscion.genai_pg_kmp.ui.dialogs.OptionSelectionContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier.fillMaxSize()
//            .navigationBarsPadding(),
    ) { paddingValues ->
//        Column(
//            Modifier.fillMaxSize()
//        ) {

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
                onStopClick = viewModel::stopGeneratingResponse,
                onToggleRuntimeSelection = viewModel::toggleManagerSelection,
                onRuntimeSelected = viewModel::onRuntimeSelected,
                onModelSelected = viewModel::onLLMSelected,
                modelManagerState = it.modelManagerState,
                chatHistory = it.chatHistory,
            )
        }


        (uiState as? ChatUIState.Success)?.ChatScreenDialogs(
            onResetError = viewModel::resetError,
        )
    }
//    }
}


@Composable
fun ChatHistoryContent(
    modifier: Modifier,
    modelManagerState: ModelManagerState,
    chatHistory: ChatHistory,
    inputFieldState: TextFieldState,
    onToggleModelSelection: () -> Unit,
    onAttachMediaClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    toggleManagerSelection: () -> Unit,
    onToggleRuntimeSelection: () -> Unit,
    onModelSelected: (Model) -> Unit,
    onRuntimeSelected: (ModelManagerOption) -> Unit,
) {
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
                    }

                    LazyColumn(
                        Modifier.fillMaxWidth()
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.shapes.medium
                            ),
                        state = rememberLazyListState(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(
                            vertical = 12.dp
                        )
                    ) {
                        itemsIndexed(
                            chatHistory.history,
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
                                    ) {
                                        Text(item.message)
                                    }
                                }
                            }
                        }
                    }

                    ChatInput(
                        state = inputFieldState,
                        isGeneratingResponse = modelManagerState.isGeneratingResponse,
                        onAttachMediaClick = onAttachMediaClick,
                        onSendClick = onSendClick,
                        onStopClick = onStopClick,
                    )
                }

                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                ) {
                    val scope = rememberCoroutineScope()
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
                                    onDismiss = onToggleRuntimeSelection,
                                    onRuntimeSelection = {
                                        onRuntimeSelected(it)
                                        onToggleRuntimeSelection()
                                        scope.launch(Dispatchers.Main.immediate) {
                                            delay(100)
                                            onToggleModelSelection()
                                        }
                                    },
                                    getTitle = { name },
                                    isSelected = { this == it },
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Model selection dialog
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
                                            .heightIn(max = 500.dp)
                                            .sharedBounds(
                                                rememberSharedContentState("optionSelectionContent2"),
                                                LocalAnimatedVisibilityScope.current!!,
                                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                            )
                                    },
                                    title = "Select Model",
                                    options = modelManagerState.llmList ?: emptyList(),
                                    selectedOption = modelManagerState.selectedLLM,
                                    onDismiss = onToggleModelSelection,
                                    onRuntimeSelection = {
                                        onModelSelected(it)
                                        onToggleModelSelection()
                                    },
                                    getTitle = { name },
                                    isSelected = { this == it },
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

@Composable
fun ChatUIState.Success.ChatScreenDialogs(
    onResetError: () -> Unit,
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
}