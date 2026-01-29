@file:OptIn(ExperimentalMaterial3Api::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devscion.genai_pg_kmp.domain.model.Model
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption
import com.devscion.genai_pg_kmp.ui.components.ChatBubble
import com.devscion.genai_pg_kmp.ui.components.ChatInput
import com.devscion.genai_pg_kmp.ui.components.OptionSelectionDialog
import com.devscion.genai_pg_kmp.ui.components.SelectionButton
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier.fillMaxSize()
            .navigationBarsPadding(),
    ) {
        (uiState as? ChatUIState.Success)?.ChatHistoryContent(
            Modifier
                .fillMaxSize()
                .padding(it),
            inputFieldState = viewModel.inputFieldState,
            onToggleRuntimeSelection = viewModel::toggleManagerSelection,
            onRuntimeSelected = viewModel::onRuntimeSelected,
            onSendClick = {
                if (viewModel.inputFieldState.text.isNotEmpty()) {
                    focusManager.clearFocus(force = true)
                }
                viewModel.onSend()
            },
            onAttachMediaClick = viewModel::onAttachMedia,
            toggleManagerSelection = viewModel::toggleManagerSelection,
            onModelSelected = viewModel::onLLMSelected,
            onToggleModelSelection = viewModel::toggleModelSelection,
            onStopClick = viewModel::stopGeneratingResponse,
        )
    }
}


@Composable
fun ChatUIState.Success.ChatHistoryContent(
    modifier: Modifier,
    inputFieldState: TextFieldState,
    onToggleRuntimeSelection: () -> Unit,
    onToggleModelSelection: () -> Unit,
    onAttachMediaClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    toggleManagerSelection: () -> Unit,
    onRuntimeSelected: (ModelManagerOption) -> Unit,
    onModelSelected: (Model) -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectionButton(
                modifier = Modifier.weight(1f),
                title = modelManagerState.selectedManager?.managerName ?: "Select Runtime",
                isSelected = false,
                onClick = toggleManagerSelection,
            )
            AnimatedVisibility(
                modelManagerState.selectedManager != null,
                modifier = Modifier.weight(1f)
            ) {
                SelectionButton(
                    modifier = Modifier.weight(1f),
                    title = if (modelManagerState.selectedLLM == null && modelManagerState.llmList.isNullOrEmpty()) "Unavailable"
                    else modelManagerState.selectedLLM?.name ?: "Select LLM",
                    isSelected = false,
                    onClick = onToggleModelSelection,
                )
            }
        }
        LazyColumn(
            modifier.fillMaxWidth()
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
            itemsIndexed(chatHistory.history, key = { _, i -> i.id }) { index, item ->
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

    if (modelManagerState.showManagerSelection) {
        OptionSelectionDialog(
            title = "Select Runtime",
            options = modelManagerState.modelManagerOptions,
            selectedOption = modelManagerState.selectedManager,
            onDismiss = onToggleRuntimeSelection,
            onRuntimeSelection = {
                onRuntimeSelected(it)
                onToggleModelSelection()
                onToggleRuntimeSelection()
            },
            getTitle = { name },
            isSelected = { this == it },
        )
    }

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

    if (modelManagerState.showModelSelection
        && modelManagerState.llmList.isNullOrEmpty().not()
    ) {
        OptionSelectionDialog(
            title = "Select Model",
            options = modelManagerState.llmList,
            selectedOption = modelManagerState.selectedLLM,
            onDismiss = onToggleModelSelection,
            onRuntimeSelection = {
                onModelSelected(it)
                onToggleModelSelection()
            },
            getTitle = { name },
            isSelected = { this == it },
        )
    }
}