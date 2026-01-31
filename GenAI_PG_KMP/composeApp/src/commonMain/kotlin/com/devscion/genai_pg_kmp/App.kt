package com.devscion.genai_pg_kmp

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import com.devscion.genai_pg_kmp.ui.ChatScreen

val LocalTransitionScope = compositionLocalOf<SharedTransitionScope?> {
    null
}

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> {
    null
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        ChatScreen()
    }
}