@file:OptIn(ExperimentalForeignApi::class)

package com.devscion.genai_pg_kmp.domain

import com.devscion.genai_pg_kmp.domain.model.Model
import kotlinx.cinterop.ExperimentalForeignApi

interface SwiftModelManager {

    fun loadModel(model: Model): Boolean

    fun sizeInTokens(text: String): Int

    fun close()

    fun stopResponseGeneration()

    suspend fun generateResponseAsync(
        inputText: String,
        attachments: List<PlatformFile>,
        progress: (partialResponse: String) -> Unit,
        completion: (completeResponse: String, error: String?) -> Unit
    )
}