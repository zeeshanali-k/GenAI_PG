package com.devscion.genai_pg_kmp.utils

import android.content.Context
import com.devscion.genai_pg_kmp.domain.model.Model

fun Model.modelPath(context: Context): String {
    val modelFile = context.getExternalFilesDir("llm") ?: return ""
    if (modelFile.exists()) {
        return "${modelFile.absolutePath}/$id"
    }
    return ""
}