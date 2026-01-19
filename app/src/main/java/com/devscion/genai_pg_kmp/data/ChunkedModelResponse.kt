package com.devscion.genai_pg_kmp.data

data class ChunkedModelResponse(
    val isDone: Boolean,
    val chunk: String,
)