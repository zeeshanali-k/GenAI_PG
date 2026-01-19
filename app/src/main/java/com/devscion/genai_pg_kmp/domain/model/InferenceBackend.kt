package com.devscion.genai_pg_kmp.domain.model

import com.google.ai.edge.litertlm.Backend
import com.google.mediapipe.tasks.genai.llminference.LlmInference

enum class InferenceBackend {
    CPU, GPU
}

fun InferenceBackend.toLiteRTLMBackend(): Backend = when (this) {
    InferenceBackend.CPU -> Backend.CPU
    InferenceBackend.GPU -> Backend.GPU
}

fun InferenceBackend.toMediaPipeBackend(): LlmInference.Backend = when (this) {
    InferenceBackend.CPU -> LlmInference.Backend.CPU
    InferenceBackend.GPU -> LlmInference.Backend.GPU
}