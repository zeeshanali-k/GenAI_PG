@file:OptIn(ExperimentalMaterial3Api::class)

package com.devscion.genai_pg_kmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devscion.genai_pg_kmp.ui.components.ChatBubble
import com.devscion.genai_pg_kmp.ui.components.LLMRuntimeSelection
import com.devscion.genai_pg_kmp.ui.components.LLMRuntimeSelectionDialog
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    Scaffold(
        modifier.fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text("GenAI PG")
                },
                navigationIcon = {
                    (uiState as? ChatUIState.Success)?.let {
                        LLMRuntimeSelection(
                            modelManagerOption = it.modelManagerState.selectedManager,
                            isSelected = false,
                            onClick = {
                                viewModel.toggleManagerSelection()
                            },
                        )
                    }
                }
            )
        }
    ) {
        (uiState as? ChatUIState.Success)?.ChatHistoryContent(
            Modifier
                .padding(it),
            onToggleRuntimeSelection = viewModel::toggleManagerSelection,
        )
    }
}


@Composable
fun ChatUIState.Success.ChatHistoryContent(
    modifier: Modifier,
    onToggleRuntimeSelection: () -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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

    if (modelManagerState.showManagerSelection) {
        LLMRuntimeSelectionDialog(
            modelManagerOptions = modelManagerState.modelManagerOptions,
            selectedModelManagerOption = modelManagerState.selectedManager,
            onDismiss = onToggleRuntimeSelection,
        )
    }
}