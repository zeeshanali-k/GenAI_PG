package com.devscion.genai_pg_kmp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.devscion.genai_pg_kmp.data.model_managers.MediaPipeModelManager
import com.devscion.genai_pg_kmp.ui.theme.LLMsPGTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LLMsPGTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modelManager = retain {
                        MediaPipeModelManager(
                            applicationContext,
                            MediaPipeModelManager.MODELS_LIST.first()
                        )
                    }
                    val response = rememberSaveable {
                        mutableStateOf("")
                    }
                    val scrollState = rememberLazyListState()
                    LaunchedEffect(Unit) {
                        modelManager.loadModel()
                        modelManager.sendPromptToLLM("Can I build RAG system using MediaPipe or LiteRT-LM?")
                            .collectLatest {
                                Log.d("LLMResponse", "chunkedResponse-> $it")
                                response.value += it.chunk
                                if (it.isDone) {
                                    Log.d("LLMResponse","Done-> ${response.value.length}")
                                }
                                launch {
                                    scrollState.animateScrollBy(20f)
                                }
                            }
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            modelManager.close()
                        }
                    }
                    LazyColumn(
                        Modifier
                            .fillMaxSize(),
                        state = scrollState,
                    ) {
                        item {
                            Text(
                                text = response.value,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}