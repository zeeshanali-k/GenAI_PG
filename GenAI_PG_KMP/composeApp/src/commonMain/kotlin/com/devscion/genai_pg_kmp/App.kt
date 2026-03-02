package com.devscion.genai_pg_kmp

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devscion.genai_pg_kmp.domain.model.Platform
import com.devscion.genai_pg_kmp.ui.ChatScreen
import com.devscion.genai_pg_kmp.ui.screens.Chat

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
        val navController = rememberNavController()

        NavHost(
            navController,
            startDestination = Chat
        ) {

            composable<Chat> {
                ChatScreen()
            }
        }
    }
}

expect fun getPlatform(): Platform