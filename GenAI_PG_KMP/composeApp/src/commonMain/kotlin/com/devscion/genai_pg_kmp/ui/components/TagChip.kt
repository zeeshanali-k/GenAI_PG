package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    title: String,
) {

    Text(
        title,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(6.dp),
        style = MaterialTheme.typography.bodyMedium,
    )

}


@Composable
@Preview
fun TagChipPreview() {
    TagChip(
        title = "LiteRT-LM"
    )
}