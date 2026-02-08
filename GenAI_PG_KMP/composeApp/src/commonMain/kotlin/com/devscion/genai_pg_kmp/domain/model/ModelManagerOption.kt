package com.devscion.genai_pg_kmp.domain.model

enum class ModelManagerOption(
    val id: String,
    val managerName: String,
    val type: ModelManagerRuntime,
    val features: List<ModelManagerRuntimeFeature>,
) {
    MEDIA_PIPE(
        id = "media_pipe",
        managerName = "MediaPipe",
        type = ModelManagerRuntime.MEDIA_PIPE,
        features = listOf(
            ModelManagerRuntimeFeature.TEXT,
            ModelManagerRuntimeFeature.VISION,
            ModelManagerRuntimeFeature.AUDIO,
            ModelManagerRuntimeFeature.RAG,
        )
    ),
    LITE_RT_LM(
        id = "lite_rt_lm",
        managerName = "LiteRT-LM",
        type = ModelManagerRuntime.LITE_RT_LM,
        features = listOf(
            ModelManagerRuntimeFeature.TEXT,
            ModelManagerRuntimeFeature.VISION,
            ModelManagerRuntimeFeature.AUDIO,
            ModelManagerRuntimeFeature.RAG,
        )
    ),
    Llama_TIK(
        id = "llamatik", managerName = "LlamaTik", type = ModelManagerRuntime.LlamaTIK,
        features = listOf(
            ModelManagerRuntimeFeature.TEXT,
            ModelManagerRuntimeFeature.RAG,
        )
    )
}


fun iOSOptions() = listOf(ModelManagerOption.MEDIA_PIPE, ModelManagerOption.Llama_TIK)

fun androidOptions() = listOf(
    ModelManagerOption.MEDIA_PIPE,
    ModelManagerOption.LITE_RT_LM,
    ModelManagerOption.Llama_TIK,
)