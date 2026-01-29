package com.devscion.genai_pg_kmp.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.plainClickable(onClick: () -> Unit) = clickable(
    onClick = onClick,
    interactionSource = remember { MutableInteractionSource() },
    indication = null
)