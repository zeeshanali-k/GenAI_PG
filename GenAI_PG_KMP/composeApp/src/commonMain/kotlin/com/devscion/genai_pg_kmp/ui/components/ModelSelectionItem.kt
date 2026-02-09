package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
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
                .clickable(onClick = onSelect)
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
                        text = "Loaded: ${localPath.takeLast(20)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = lookaheadModifier
                    )
                } else {
                    Text(
                        text = "Not selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = lookaheadModifier
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Info Button
            AnimatedVisibility(showInfo.not()) {
                IconButton(
                    modifier = infoIconModifier,
                    onClick = onToggleInfo
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Download Button
            if (showDownload && downloadUrl.isNullOrBlank().not()) {
                IconButton(onClick = { uriHandler.openUri(downloadUrl) }) {
                    Icon(
                        painter = painterResource(Res.drawable.rounded_downloading_24),
                        contentDescription = "Download Model",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // File Select Button
            if (showFileSelect) {
                IconButton(onClick = onFileSelect) {
                    Icon(
                        painter = painterResource(Res.drawable.rounded_file_open_24),
                        contentDescription = "Select File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
