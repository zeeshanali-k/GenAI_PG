package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devscion.genai_pg_kmp.utils.plainClickable

@Composable
fun ChatInput(
    modifier: Modifier = Modifier,
    state: TextFieldState,
    isGeneratingResponse: Boolean,
    onAttachMediaClick: () -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        BasicTextField(
            state,
            Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium,
            decorator = retain {
                TextFieldDecorator {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.shapes.medium
                            )
                            .padding(9.dp)
                    ) {
                        it()
                    }
                }
            }
        )

        Icon(
            Icons.Default.AddCircle,
            "",
            modifier = Modifier.size(42.dp)
                .clip(RoundedCornerShape(Int.MAX_VALUE.dp))
                .plainClickable(onAttachMediaClick)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(9.dp)
        )
        AnimatedContent(state.text.isNotEmpty()) {
            if (it) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "",
                    modifier = Modifier.size(42.dp)
                        .clip(RoundedCornerShape(Int.MAX_VALUE.dp))
                        .plainClickable(onSendClick)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(9.dp)
                )
            }
        }
        AnimatedContent(isGeneratingResponse) {
            if (it) {
                Icon(
                    Icons.Default.Close,
                    "",
                    modifier = Modifier.size(42.dp)
                        .clip(RoundedCornerShape(Int.MAX_VALUE.dp))
                        .plainClickable(onStopClick)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(9.dp)
                )
            }
        }


    }
}