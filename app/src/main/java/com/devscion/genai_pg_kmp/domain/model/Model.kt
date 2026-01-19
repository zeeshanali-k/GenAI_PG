package com.devscion.genai_pg_kmp.domain.model

import android.content.Context

typealias SizeMB = Int

/**
 * @param topK The number of tokens the model considers at each step of generation. Limits predictions to the top k most-probable tokens.
 * @param maxTokens The maximum number of tokens (input tokens + output tokens) the model handles.
 * @param temperature The amount of randomness introduced during generation.
 * A higher temperature results in more creativity in the generated text,
 * while a lower temperature produces more predictable generation.
 */

data class Model(
    val id: String,
    val name: String,
    val size: SizeMB,
    val backend: InferenceBackend = InferenceBackend.CPU,
    val topK: Int = 64,
    val maxTokens: Int = 512,
    val temperature: Float = 0.5f,
    val topP: Float = 0.95f,
    val randomSeed: Int = 0,
) {

    fun modelPath(context: Context): String {
        val modelFile = context.getExternalFilesDir("llm") ?: return ""
        if (modelFile.exists()) {
            return "${modelFile.absolutePath}/$id"
        }
        return ""
    }
}