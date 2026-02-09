package com.devscion.genai_pg_kmp.data

import android.content.Context
import androidx.core.content.edit
import com.devscion.genai_pg_kmp.domain.ModelSettings

class AndroidModelSettings(context: Context) : ModelSettings {
    private val prefs = context.getSharedPreferences("model_settings", Context.MODE_PRIVATE)

    override fun savePath(id: String, path: String?) {
        prefs.edit { putString(id, path) }
    }

    override fun getPath(id: String): String? {
        return prefs.getString(id, null)
    }
}
