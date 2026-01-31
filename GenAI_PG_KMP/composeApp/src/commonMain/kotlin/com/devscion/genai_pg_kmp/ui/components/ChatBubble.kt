package com.devscion.genai_pg_kmp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ChatBubble(
    modifier: Modifier = Modifier,
    index: Int,
    isSent: Boolean,
    content: @Composable () -> Unit
) {

    var isLoaded by rememberSaveable() {
        mutableStateOf(false)
    }
    val animationFactor by animateFloatAsState(
        if (isLoaded) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
    )
    val visibility by animateFloatAsState(
        if (isLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
    )
    LaunchedEffect(Unit) {
        delay(index * 60L)
        isLoaded = true
    }
    val color = if (isSent) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
    Box(
        modifier
            .drawWithCache {
                onDrawWithContent {
                    this.withTransform({
                        this.rotate(if (isSent) 180f else 0f)
                    }) {
                        val path = Path().apply {
                            val maxWidth = size.width
                            val maxHeight = size.height
                            this.moveTo(0f, 0f)
                            this.lineTo(20f, 5f)
                            this.lineTo((maxWidth * animationFactor) - 20f, 0f)
                            this.quadraticTo(
                                (maxWidth * animationFactor),
                                0f,
                                (maxWidth * animationFactor),
                                20f
                            )
                            this.lineTo(
                                maxWidth * animationFactor,
                                (animationFactor * maxHeight) - 20f,
                            )
                            this.quadraticTo(
                                (maxWidth * animationFactor),
                                animationFactor * maxHeight,
                                (maxWidth * animationFactor) - 20,
                                animationFactor * maxHeight
                            )
                            this.lineTo(40f, animationFactor * maxHeight)

                            this.quadraticTo(
                                (15f * animationFactor),
                                animationFactor * maxHeight,
                                (15f * animationFactor),
                                (animationFactor * maxHeight) - 20f
                            )
                            this.lineTo(20f, 30f)
                            this.close()
                        }
                        drawPath(
                            path, color
                        )
                    }

                    drawContent()
                }
            }, contentAlignment = if (isSent) Alignment.CenterEnd
        else Alignment.CenterStart
    ) {
        Box(
            Modifier.alpha(visibility), contentAlignment = if (isSent) Alignment.CenterEnd
            else Alignment.CenterStart
        ) {
            content()
        }
    }

}

@Composable
@Preview
fun ChatBubblePreview() {
    ChatBubble(modifier = Modifier.fillMaxWidth(), index = 1, isSent = false) {
        Column(
            Modifier.fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("Hi from Gemini Nano\n\n")
        }
    }
}