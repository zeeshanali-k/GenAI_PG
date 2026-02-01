package com.devscion.genai_pg_kmp.domain.model

typealias SizeMB = Int

/**
 * @param topK The number of tokens the model considers at each step of generation. Limits predictions to the top k most-probable tokens.
 * @param maxTokens The maximum number of tokens (input tokens + output tokens) the model handles.
 * @param temperature The amount of randomness introduced during generation. A higher temperature results in more creativity in the generated text, while a lower temperature produces more predictable generation.
 */

data class Model(
    val id: String,
    val name: String,
    val size: SizeMB,
    val backend: InferenceBackend = InferenceBackend.CPU,
    val topK: Int = 64,
    val maxTokens: Int = 512,
    val temperature: Float = 0.5f,
    val topP: Float = 0.95f,
    val randomSeed: Int = 0,
    val modelType: ModelManagerRuntime,
) {
    companion object {
        fun models(type: ModelManagerRuntime, platform: Platform): List<Model> {
            val isIOS = platform == Platform.IOS
            return when (type) {
                ModelManagerRuntime.LITE_RT_LM -> if (isIOS) emptyList()
                else LITE_RT_LM_MODELS

                ModelManagerRuntime.MEDIA_PIPE -> if (isIOS) MEDIA_PIPE_MODELS_IOS
                else MEDIA_PIPE_MODELS

                ModelManagerRuntime.LlamaTIK -> LlAMATIK_MODELS
            }
        }

        private val LlAMATIK_MODELS = buildList {
            val modelType = ModelManagerRuntime.LlamaTIK
            add(
                Model(
                    id = "LFM2.5-1.2B-Thinking-Q8_0.gguf",
                    name = "LFM2.5 1.2B",
                    size = 1500,
                    modelType = modelType,
                ),
            )
            add(
                Model(
                    id = "phi-2.Q4_0.gguf",
                    name = "phi-2 Q4_0",
                    size = 1500,
                    modelType = modelType,
                ),
            )
            add(
                Model(
                    id = "Gemini-Nano-Gemmafied.Q3_K_M.gguf",
                    name = "Gemini-Nano Q3",
                    size = 1600,
                    modelType = modelType,
                )
            )
        }
        private val LITE_RT_LM_MODELS = buildList {
            val modelType = ModelManagerRuntime.LITE_RT_LM
            add(
                Model(
                    id = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
                    name = "Qwen2.5 1.5B",
                    size = 1600,
                    modelType = modelType,
                )
            )
        }
        private val MEDIA_PIPE_MODELS_IOS = buildList {
            val modelType = ModelManagerRuntime.MEDIA_PIPE
            add(
                Model(
                    id = "gemma-1.1-2b-it-gpu-int4.bin",
                    name = "Gemma-1.1 2B IT (GPU)",
                    size = 1070,
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "gemma-1.1-2b-it-cpu-int4.bin",
                    name = "Gemma-1.1 2B IT (CPU)",
                    size = 1070,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "gemma2-2b-it-gpu-int8.bin",
                    name = "Gemma-1.1 2B IT (GPU)",
                    size = 1070,
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType
                )
            )
        }
        private val MEDIA_PIPE_MODELS = buildList {
            val modelType = ModelManagerRuntime.MEDIA_PIPE

            add(
                Model(
                    id = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
                    name = "Gemma3 1B IT (CPU)",
                    size = 1070,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
                    name = "Gemma3 1B IT (GPU)",
                    size = 1070,
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task",
                    name = "Gemma2 2B IT",
                    size = 2600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 50,
                    topP = 0.9f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
                    name = "DeepSeek R1 Distill Qwen 1.5B",
                    size = 1600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 40,
                    topP = 0.7f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Llama-3.2-1B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Llama 3.2 1B Instruct",
                    size = 1200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 64,
                    topP = 0.9f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Llama 3.2 3B Instruct",
                    size = 3200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 64,
                    topP = 0.9f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Phi-4 Mini Instruct",
                    size = 3600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Qwen2.5 0.5B Instruct",
                    size = 600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Qwen2.5 1.5B Instruct",
                    size = 1600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Qwen2.5 3B Instruct",
                    size = 3200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "SmolLM 135M Instruct",
                    size = 200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType
                )
            )
            add(
                Model(
                    id = "TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
                    name = "TinyLlama 1.1B Chat v1.0",
                    size = 1100,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType
                )
            )
        }
        private val ALL_MODELS_ANDROID = LITE_RT_LM_MODELS + MEDIA_PIPE_MODELS + LlAMATIK_MODELS
        private val ALL_MODELS_IOS = MEDIA_PIPE_MODELS_IOS
    }
}