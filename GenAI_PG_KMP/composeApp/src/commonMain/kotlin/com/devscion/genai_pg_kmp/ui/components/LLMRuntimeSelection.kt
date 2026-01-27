package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devscion.genai_pg_kmp.domain.model.ModelManagerOption

@Composable
fun LLMRuntimeSelection(
    modifier: Modifier = Modifier,
    modelManagerOption: ModelManagerOption?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {

    Box(
        modifier
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.shapes.medium
                )
                else Modifier
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(9.dp),
    ) {
        Text(
            modelManagerOption?.managerName ?: "Select Runtime",
            textAlign = TextAlign.Start,
        )
    }
}


@Preview
@Composable
fun LLMRuntimeSelectionNotSelectedPreview() {
    LLMRuntimeSelection(
        modelManagerOption = ModelManagerOption.MEDIA_PIPE,
        isSelected = false,
        onClick = { },
    )
}

@Preview
@Composable
fun LLMRuntimeSelectionSelectedPreview() {
    LLMRuntimeSelection(
        modelManagerOption = ModelManagerOption.MEDIA_PIPE,
        isSelected = true,
        onClick = { },
    )
}