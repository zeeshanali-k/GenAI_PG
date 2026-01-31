package com.devscion.genai_pg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devscion.genai_pg.ui.theme.LLMsPGTheme
import com.devscion.genai_pg_kmp.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LLMsPGTheme {
                App()
            }
        }
    }
}