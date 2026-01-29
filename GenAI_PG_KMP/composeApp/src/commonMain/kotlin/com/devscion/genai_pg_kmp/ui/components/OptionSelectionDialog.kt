package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun <T : Any> OptionSelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T?,
    getTitle: T.() -> String,
    isSelected: T.(selectedOption: T?) -> Boolean,
    onDismiss: () -> Unit,
    onRuntimeSelection: (T) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier)
                Text(
                    title, style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    Icons.Default.Close, "",
                    modifier = Modifier.clickable(
                        onClick = onDismiss,
                        indication = null,
                        interactionSource = null
                    )
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(options) {
                    SelectionButton(
                        modifier = Modifier.fillMaxWidth(),
                        title = it.getTitle(),
                        isSelected = it.isSelected(selectedOption),
                        onClick = {
                            onRuntimeSelection(it)
                        }
                    )
                }

            }
        }
    }
}