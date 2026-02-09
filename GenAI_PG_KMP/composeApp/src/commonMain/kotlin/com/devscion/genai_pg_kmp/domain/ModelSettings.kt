package com.devscion.genai_pg_kmp.domain

interface ModelSettings {
    fun savePath(id: String, path: String?)
    fun getPath(id: String): String?
}
