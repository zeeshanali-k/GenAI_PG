package com.devscion.llmspg.data

data class ChunkedModelResponse(
    val isDone: Boolean,
    val chunk: String,
)