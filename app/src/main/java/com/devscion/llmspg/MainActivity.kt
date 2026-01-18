package com.devscion.llmspg

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devscion.llmspg.data.model_managers.MediaPipeModelManager
import com.devscion.llmspg.ui.theme.LLMsPGTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LLMsPGTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modelManager = retain {
                        MediaPipeModelManager(applicationContext, MediaPipeModelManager.MODELS_LIST.first())
                    }
                    val response = rememberSaveable {
                        mutableStateOf("")
                    }
                    LaunchedEffect(Unit) {
                        modelManager.loadModel()
                        modelManager.sendPromptToLLM("HI, how are you?")
                    }
                    LaunchedEffect(Unit) {
                        modelManager.subscribeResponseFlow().collectLatest {
                            Log.d("LLMResponse", "chunkedResponse-> $it")
                            response.value += it.chunk
                        }
                    }
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LLMsPGTheme {
        Greeting("Android")
    }
}