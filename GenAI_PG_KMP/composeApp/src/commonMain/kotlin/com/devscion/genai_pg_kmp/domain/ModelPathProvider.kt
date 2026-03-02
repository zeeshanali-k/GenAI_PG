package com.devscion.genai_pg_kmp.domain

typealias FilePath = String

interface ModelPathProvider {

    fun getPath(modelName: String): String?
    suspend fun resolvePath(path: String): String?
    suspend fun makeLocalCopy(path: FilePath): FilePath?
    suspend fun getContentByteArray(path: FilePath): ByteArray?
    suspend fun getContentText(path: FilePath): String?

}