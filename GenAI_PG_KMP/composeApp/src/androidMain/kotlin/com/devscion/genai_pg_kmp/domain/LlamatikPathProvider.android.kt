package com.devscion.genai_pg_kmp.domain

import android.content.Context

class LlamatikPathProviderAndroid(
    private val context: Context,
) : LlamatikPathProvider {

    override fun getPath(modelName: String): String? {
        return try {
            val modelFile = context.getExternalFilesDir("llm") ?: return ""
            if (modelFile.exists()) {
                return "${modelFile.absolutePath}/${modelName}"
            }
            null
        } catch (_: Exception) {
            null
        }
    }

}