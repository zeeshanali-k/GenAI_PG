package com.devscion.llmspg.data

import android.content.Context

typealias SizeMB = Int

data class Model(
    val id: String,
    val name: String,
    val size: SizeMB,
) {

    fun modelPath(context: Context): String {
        val modelFile = context.getExternalFilesDir("llm") ?: return ""
        if (modelFile.exists()) {
            return "${modelFile.absolutePath}/$id"
        }
        return ""
    }
}