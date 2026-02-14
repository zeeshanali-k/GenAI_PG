package com.devscion.genai_pg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devscion.genai_pg.ui.theme.LLMsPGTheme
import com.devscion.genai_pg_kmp.App
import com.devscion.genai_pg_kmp.data.AndroidFilePicker
import com.devscion.genai_pg_kmp.domain.FilePicker
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val filePicker by inject<FilePicker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (filePicker as AndroidFilePicker).register(this)

        enableEdgeToEdge()
        setContent {
            LLMsPGTheme {
                App()
            }
        }
    }
}