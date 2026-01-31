package com.devscion.genai_pg_kmp.domain.model

data class ChunkedModelResponse(
    val isDone: Boolean,
    val chunk: String,
)