package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SelectionButton(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        title,
        textAlign = TextAlign.Start,
        modifier =
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
                .padding(9.dp)
    )
}


@Preview
@Composable
fun SelectionButtonNotSelectedPreview() {
    SelectionButton(
        title = "Not Selected",
        isSelected = false,
        onClick = { },
    )
}

@Preview
@Composable
fun SelectionButtonSelectedPreview() {
    SelectionButton(
        title = "Selected",
        isSelected = true,
        onClick = { },
    )
}