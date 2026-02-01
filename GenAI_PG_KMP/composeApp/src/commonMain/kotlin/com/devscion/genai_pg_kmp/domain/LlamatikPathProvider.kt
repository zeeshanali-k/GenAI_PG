package com.devscion.genai_pg_kmp.domain

interface LlamatikPathProvider {

    fun getPath(modelName: String): String?

}