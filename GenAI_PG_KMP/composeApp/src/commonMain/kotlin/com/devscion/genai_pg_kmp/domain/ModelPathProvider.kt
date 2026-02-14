package com.devscion.genai_pg_kmp.domain

interface ModelPathProvider {

    fun getPath(modelName: String): String?
    suspend fun resolvePath(path: String): String?

}