package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devscion.genai_pg_kmp.LocalTransitionScope
import genai_pg.genai_pg_kmp.composeapp.generated.resources.Res
import genai_pg.genai_pg_kmp.composeapp.generated.resources.rounded_downloading_24
import genai_pg.genai_pg_kmp.composeapp.generated.resources.rounded_file_open_24
import org.jetbrains.compose.resources.painterResource

@Composable
fun ModelSelectionItem(
    name: String,
    downloadUrl: String?,
    tags: List<String>,
    localPath: String?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFileSelect: () -> Unit,
    onToggleInfo: () -> Unit,
    infoIconModifier: Modifier = Modifier,
    showStatus: Boolean = true,
    showInfo: Boolean = true,
    showDownload: Boolean = true,
    showFileSelect: Boolean = true,
) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.shapes.medium
                )
                else Modifier
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
                .clickable(onClick = onSelect),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val lookaheadModifier = with(LocalTransitionScope.current!!) {
                Modifier.skipToLookaheadSize()
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = lookaheadModifier
            )
            if (showStatus) {
                if (localPath != null) {
                    Text(
                        text = "File Loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = lookaheadModifier
                    )
                } else {
                    Text(
                        text = "File Not selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = lookaheadModifier
                    )
                }
            }
            if (tags.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach {
                        TagChip(title = it)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Info Button
            AnimatedVisibility(showInfo.not()) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = infoIconModifier
                        .clickable(
                            onClick = onToggleInfo,
                            interactionSource = null,
                            indication = null
                        )
                )
            }

            // Download Button
            if (showDownload && downloadUrl.isNullOrBlank().not()) {
                Icon(
                    painter = painterResource(Res.drawable.rounded_downloading_24),
                    contentDescription = "Download Model",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        onClick = { uriHandler.openUri(downloadUrl) },
                        interactionSource = null,
                        indication = null
                    )
                )
            }

            // File Select Button
            if (showFileSelect) {
                Icon(
                    painter = painterResource(Res.drawable.rounded_file_open_24),
                    contentDescription = "Select File",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        onClick = onFileSelect,
                        interactionSource = null,
                        indication = null
                    )
                )
            }
        }
    }
}

@Composable
@Preview
fun ModelSelectionItemPreview() {
    ModelSelectionItem(
        name = "LiteRT-LM",
        downloadUrl = null,
        tags = listOf("LiteRT", "LM"),
        localPath = null,
        isSelected = false,
        onSelect = {},
        onFileSelect = {},
        onToggleInfo = {},
    )
}

@Composable
@Preview
fun ModelSelectionItemPreviewSelected() {
    ModelSelectionItem(
        name = "LiteRT-LM",
        downloadUrl = null,
        tags = listOf("LiteRT", "LM"),
        localPath = null,
        isSelected = true,
        onSelect = {},
        onFileSelect = {},
        onToggleInfo = {},
    )
}