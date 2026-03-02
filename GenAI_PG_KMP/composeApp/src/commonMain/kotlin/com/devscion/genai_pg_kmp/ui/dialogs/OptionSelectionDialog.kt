package com.devscion.genai_pg_kmp.ui.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devscion.genai_pg_kmp.LocalAnimatedVisibilityScope
import com.devscion.genai_pg_kmp.LocalTransitionScope
import com.devscion.genai_pg_kmp.ui.components.ModelSelectionItem
import genai_pg.genai_pg_kmp.composeapp.generated.resources.Res
import genai_pg.genai_pg_kmp.composeapp.generated.resources.supported_formats
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T : Any> OptionSelectionContent(
    modifier: Modifier = Modifier,
    title: String,
    options: List<T>,
    selectedOption: T?,
    getTags: T.() -> List<String>,
    getFormats: T.() -> List<String> = { emptyList() },
    getName: T.() -> String,
    getDescription: T.() -> String,
    getDownloadUrl: T.() -> String?,
    getLocalPath: T.() -> String?,
    onDismiss: () -> Unit,
    onSelect: T.() -> Unit,
    onFileSelect: T.() -> Unit,
    showStatus: Boolean = true,
    showDownload: Boolean = true,
    showFileSelect: Boolean = true,
    maxListHeight: Dp = 400.dp,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.clickable(onClick = onDismiss)
            )
        }


        SharedTransitionLayout {

            CompositionLocalProvider(
                LocalTransitionScope provides this@SharedTransitionLayout,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { item ->
                        val infoKey = "info-${getName(item)}-${item.hashCode()}"
                        var showInfo by remember(infoKey) { mutableStateOf(false) }

                        AnimatedContent(
                            showInfo,
                            transitionSpec = { fadeIn().togetherWith(fadeOut()) }
                        ) {
                            CompositionLocalProvider(
                                LocalAnimatedVisibilityScope provides this,
                            ) {
                                val transitionScope = LocalTransitionScope.current
                                val visibilityScope = LocalAnimatedVisibilityScope.current
                                Column(Modifier.fillMaxWidth()) {
                                    ModelSelectionItem(
                                        name = item.getName(),
                                        showInfo = showInfo,
                                        tags = item.getTags(),
                                        downloadUrl = item.getDownloadUrl(),
                                        localPath = item.getLocalPath(),
                                        isSelected = item == selectedOption,
                                        onSelect = { onSelect(item) },
                                        onFileSelect = { item.onFileSelect() },
                                        onToggleInfo = { showInfo = showInfo.not() },
                                        infoIconModifier = if (transitionScope != null && visibilityScope != null) {
                                            with(transitionScope) {
                                                Modifier.sharedElement(
                                                    rememberSharedContentState(infoKey),
                                                    visibilityScope,
                                                    //                                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                                )
                                            }
                                        } else {
                                            Modifier
                                        },
                                        showStatus = showStatus,
                                        showDownload = showDownload,
                                        showFileSelect = showFileSelect,
                                    )

                                    if (it) {
                                        ModelInfoCard(
                                            description = getDescription(item),
                                            infoKey = infoKey,
                                            formats = getFormats(item),
                                            onToggleInfo = {
                                                showInfo = showInfo.not()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelInfoCard(
    modifier: Modifier = Modifier,
    description: String,
    infoKey: String,
    formats: List<String>,
    onToggleInfo: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(shape)
            .background(gradient)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = shape
            )
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = with(LocalTransitionScope.current!!) {
                    Modifier.sharedElement(
                        rememberSharedContentState(infoKey),
                        LocalAnimatedVisibilityScope.current!!,
//                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    )
                }
            )
            Text(
                text = "Details",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                modifier = Modifier,
                onClick = onToggleInfo
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "close",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (formats.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${stringResource(Res.string.supported_formats)}: ",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(6.dp)
                )
                formats.forEach {
                    Text(
                        it, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
