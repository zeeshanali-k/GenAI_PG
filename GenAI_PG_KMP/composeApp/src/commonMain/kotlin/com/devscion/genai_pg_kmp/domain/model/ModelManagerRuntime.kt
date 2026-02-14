package com.devscion.genai_pg_kmp.domain.model

enum class ModelManagerRuntime {
    LITE_RT_LM, MEDIA_PIPE, LlamaTIK
}

enum class ModelManagerRuntimeFeature(
    val title: String,
) {
    TEXT("Text"), RAG("RAG"), VISION("Vision"), AUDIO("Audio")
}
