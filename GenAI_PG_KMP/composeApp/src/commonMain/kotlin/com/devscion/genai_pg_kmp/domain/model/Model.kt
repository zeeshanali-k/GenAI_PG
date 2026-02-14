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
    val maxTokens: Int = 1024,
    val temperature: Float = 0.5f,
    val topP: Float = 0.95f,
    val randomSeed: Int = 0,
    val features: List<ModelManagerRuntimeFeature> = listOf(ModelManagerRuntimeFeature.TEXT),
    val modelType: ModelManagerRuntime,
//    val embeddingModel: String = "embeddinggemma-300M_seq256_mixed-precision.tflite",
//    val tokenizerModel: String = "sentencepiece.model",
    val downloadUrl: String,
    val description: String,
    val localPath: String? = null,
    val supportedFormats: List<ModelSupportedFormat> = buildList {
        if (modelType == ModelManagerRuntime.MEDIA_PIPE) {
            add(ModelSupportedFormat.LITERT)
            add(ModelSupportedFormat.TASK)
        }
        if (modelType == ModelManagerRuntime.LlamaTIK) {
            add(ModelSupportedFormat.BIN)
        }
        if (modelType == ModelManagerRuntime.LITE_RT_LM) {
            add(ModelSupportedFormat.LITERT)
        }
    }
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


        val MEDIA_PIPE_MODELS = buildList {
            val modelType = ModelManagerRuntime.MEDIA_PIPE

            // MULTIMODAL MODELS (Vision-enabled)
            add(
                Model(
                    id = "gemma-3n-E2B-it-int4.task",
                    name = "Gemma 3n E2B (Multimodal)",
                    size = 2100,
                    modelType = modelType,
                    features = listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm",
                    description = "Effective 2B multimodal model supporting text, image, video, and audio input"
                )
            )
            add(
                Model(
                    id = "gemma-3n-E4B-it-int4.task",
                    name = "Gemma 3n E4B (Multimodal)",
                    size = 3400,
                    modelType = modelType,
                    features = listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm",
                    description = "Effective 4B multimodal model with MobileNet-V5 vision encoder"
                )
            )

            // TEXT-ONLY MODELS
            add(
                Model(
                    id = "gemma3-1B-it-int4.task",
                    name = "Gemma 3 1B Instruct",
                    size = 529,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
                    description = "Lightweight 1B text-only model from Gemma 3 family"
                )
            )
            add(
                Model(
                    id = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
                    name = "Gemma 3 1B IT Q8 (CPU)",
                    size = 1070,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
                    description = "Q8 quantized with extended KV cache"
                )
            )
            add(
                Model(
                    id = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
                    name = "Gemma 3 1B IT Q8 (GPU)",
                    size = 1070,
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
                    description = "GPU-optimized Q8 quantization"
                )
            )
            add(
                Model(
                    id = "gemma-3-270m-it-int4.task",
                    name = "Gemma 3 270M Instruct",
                    size = 200,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/gemma-3-270m-it",
                    description = "Ultra-lightweight 270M parameter model"
                )
            )
            add(
                Model(
                    id = "Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task",
                    name = "Gemma 2 2B Instruct",
                    size = 2600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 50,
                    topP = 0.9f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/google/gemma-2-2b-it",
                    description = "Gemma 2 generation with improved reasoning"
                )
            )

            // QWEN FAMILY
            add(
                Model(
                    id = "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Qwen 2.5 0.5B Instruct",
                    size = 600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct",
                    description = "Ultra-compact Qwen model for basic tasks"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Qwen 2.5 1.5B Instruct",
                    size = 1600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
                    description = "Multilingual model with strong reasoning"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Qwen 2.5 3B Instruct",
                    size = 3200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct",
                    description = "Advanced reasoning and coding capabilities"
                )
            )

            // LLAMA FAMILY
            add(
                Model(
                    id = "Llama-3.2-1B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Llama 3.2 1B Instruct",
                    size = 1200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 64,
                    topP = 0.9f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct",
                    description = "Meta's efficient 1B instruction-tuned model"
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
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct",
                    description = "Balanced performance for general tasks"
                )
            )
            add(
                Model(
                    id = "TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
                    name = "TinyLlama 1.1B Chat",
                    size = 1100,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0",
                    description = "Fast, efficient chat model"
                )
            )

            // MICROSOFT PHI FAMILY
            add(
                Model(
                    id = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "Phi-4 Mini Instruct",
                    size = 3600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct",
                    description = "Microsoft's reasoning-focused 4B model"
                )
            )

            // DEEPSEEK FAMILY
            add(
                Model(
                    id = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
                    name = "DeepSeek R1 Distill Qwen 1.5B",
                    size = 1600,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 40,
                    topP = 0.7f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
                    description = "Reasoning model distilled from DeepSeek-R1"
                )
            )

            // SMOLLM FAMILY
            add(
                Model(
                    id = "SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
                    name = "SmolLM 135M Instruct",
                    size = 200,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct",
                    description = "Smallest viable LLM for basic interactions"
                )
            )
        }

        val MEDIA_PIPE_MODELS_IOS = buildList {
            val modelType = ModelManagerRuntime.MEDIA_PIPE
            add(
                Model(
                    id = "gemma2-2b-it-gpu-int8.bin",
                    name = "Gemma2 2B IT (GPU)",
                    size = 2630,
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://www.kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-gpu-int8",
                    description = "GPU-optimized Gemma2 for iOS"
                )
            )
            add(
                Model(
                    id = "gemma-1.1-2b-it-gpu-int4.bin",
                    name = "Gemma 1.1 2B IT (GPU)",
                    size = 1070,
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://www.kaggle.com/models/google/gemma/tfLite/gemma-1.1-2b-it-gpu-int4",
                    description = "GPU-optimized Gemma 1.1 for iOS"
                )
            )
            add(
                Model(
                    id = "gemma-1.1-2b-it-cpu-int4.bin",
                    name = "Gemma 1.1 2B IT (CPU)",
                    size = 1070,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    modelType = modelType,
                    downloadUrl = "https://www.kaggle.com/models/google/gemma/tfLite/gemma-1.1-2b-it-cpu-int4",
                    description = "CPU-optimized Gemma 1.1 for iOS"
                )
            )
        }

        /**
         * LITERT-LM MODELS (.litertlm format)
         * Next-generation format with better compression and metadata
         * Optimized for NPU acceleration on Android
         * Download from: https://huggingface.co/models?library=litert-lm
         */
        val LITE_RT_LM_MODELS = buildList {
            val modelType = ModelManagerRuntime.LITE_RT_LM

            // MULTIMODAL MODELS
            add(
                Model(
                    id = "gemma-3n-E2B-it-int4.litertlm",
                    name = "Gemma 3n E2B (Multimodal)",
                    size = 2100,
                    modelType = modelType,
                    features = listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm",
                    description = "Effective 2B with vision, video & audio support. Runs on 2GB RAM"
                )
            )
            add(
                Model(
                    id = "gemma-3n-E4B-it-int4.litertlm",
                    name = "Gemma 3n E4B (Multimodal)",
                    size = 3400,
                    modelType = modelType,
                    features = listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm",
                    description = "Effective 4B with superior multimodal performance. Runs on 3GB RAM"
                )
            )
            add(
                Model(
                    id = "gemma-3n-E2B-it-int4-Web.litertlm",
                    name = "Gemma 3n E2B Web (Multimodal)",
                    size = 3040,
                    modelType = modelType,
                    features = listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                    backend = InferenceBackend.GPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm",
                    description = "Web-optimized E2B with WebGPU acceleration"
                )
            )
            add(
                Model(
                    id = "FastVLM-0.5B-int4.litertlm",
                    name = "FastVLM 0.5B (Vision)",
                    size = 500,
                    modelType = modelType,
                    features = listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/litert-community/FastVLM-0.5B",
                    description = "Ultra-fast vision-language model"
                )
            )

            // TEXT-ONLY MODELS
            add(
                Model(
                    id = "gemma3-1B-it-int4.litertlm",
                    name = "Gemma 3 1B Instruct",
                    size = 529,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT",
                    description = "Latest Gemma 3 with 32K context window"
                )
            )
            add(
                Model(
                    id = "gemma-3-270m-it-int4.litertlm",
                    name = "Gemma 3 270M Instruct",
                    size = 200,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 1.0f,
                    topK = 64,
                    topP = 0.95f,
                    downloadUrl = "https://huggingface.co/litert-community/gemma-3-270m-it",
                    description = "Smallest Gemma 3 for constrained devices"
                )
            )

            // QWEN FAMILY
            add(
                Model(
                    id = "Qwen3-0.6B-it-int4.litertlm",
                    name = "Qwen 3 0.6B",
                    size = 600,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B",
                    description = "Compact Qwen 3 with thinking mode support"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
                    name = "Qwen 2.5 0.5B Instruct",
                    size = 600,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct",
                    description = "Multilingual support in minimal footprint"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
                    name = "Qwen 2.5 1.5B Instruct",
                    size = 1600,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
                    description = "Strong reasoning and multilingual capabilities"
                )
            )

            // MICROSOFT PHI FAMILY
            add(
                Model(
                    id = "Phi-4-mini-instruct-int4.litertlm",
                    name = "Phi-4 Mini Instruct",
                    size = 3600,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 40,
                    topP = 1.0f,
                    downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct",
                    description = "Excellent for math and reasoning tasks"
                )
            )

            // DEEPSEEK FAMILY
            add(
                Model(
                    id = "DeepSeek-R1-Distill-Qwen-1.5B-int4.litertlm",
                    name = "DeepSeek R1 Distill 1.5B",
                    size = 1600,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.6f,
                    topK = 40,
                    topP = 0.7f,
                    downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
                    description = "Chain-of-thought reasoning capabilities"
                )
            )

            // TINYLLAMA
            add(
                Model(
                    id = "TinyLlama-1.1B-Chat-v1.0-int4.litertlm",
                    name = "TinyLlama 1.1B Chat",
                    size = 1100,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    downloadUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0",
                    description = "Fast inference on low-end devices"
                )
            )

            // SMOLLM
            add(
                Model(
                    id = "SmolLM-135M-Instruct-int4.litertlm",
                    name = "SmolLM 135M Instruct",
                    size = 200,
                    modelType = modelType,
                    backend = InferenceBackend.CPU,
                    temperature = 0.95f,
                    topK = 40,
                    topP = 1.0f,
                    downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct",
                    description = "Ultra-compact for basic chat"
                )
            )
        }

        /**
         * LLAMATIK MODELS (.gguf format)
         * Based on llama.cpp for maximum hardware compatibility
         * Supports CPU inference on all platforms
         * Download from: https://huggingface.co/models?library=gguf
         */
        val LlAMATIK_MODELS = buildList {
            val modelType = ModelManagerRuntime.LlamaTIK

            // GEMMA FAMILY (GGUF)
            add(
                Model(
                    id = "gemma-3-1b-it-Q4_K_M.gguf",
                    name = "Gemma 3 1B Q4_K_M",
                    size = 700,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF",
                    description = "Balanced quantization for Gemma 3 1B"
                )
            )
            add(
                Model(
                    id = "gemma-3-1b-it-Q8_0.gguf",
                    name = "Gemma 3 1B Q8_0",
                    size = 1200,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF",
                    description = "High quality Q8 quantization"
                )
            )
            add(
                Model(
                    id = "gemma-3-4b-it-Q4_K_M.gguf",
                    name = "Gemma 3 4B Q4_K_M",
                    size = 2500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/ggml-org/gemma-3-4b-it-GGUF",
                    description = "Larger Gemma 3 with vision capabilities (GGUF format)"
                )
            )
            add(
                Model(
                    id = "gemma-2-2b-it-Q4_K_M.gguf",
                    name = "Gemma 2 2B Q4_K_M",
                    size = 1500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF",
                    description = "Previous generation Gemma"
                )
            )

            // QWEN FAMILY (GGUF)
            add(
                Model(
                    id = "Qwen3-0.6B-Q4_K_M.gguf",
                    name = "Qwen 3 0.6B Q4_K_M",
                    size = 400,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF",
                    description = "Latest Qwen 3 with thinking mode"
                )
            )
            add(
                Model(
                    id = "Qwen3-4B-Q4_K_M.gguf",
                    name = "Qwen 3 4B Q4_K_M",
                    size = 2500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen3-4B-GGUF",
                    description = "Advanced reasoning and multilingual"
                )
            )
            add(
                Model(
                    id = "Qwen3-8B-Q4_K_M.gguf",
                    name = "Qwen 3 8B Q4_K_M",
                    size = 5000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen3-8B-GGUF",
                    description = "High-performance Qwen model"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
                    name = "Qwen 2.5 1.5B Q4_K_M",
                    size = 1000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF",
                    description = "Excellent multilingual small model"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
                    name = "Qwen 2.5 3B Q4_K_M",
                    size = 2000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF",
                    description = "Strong coding and reasoning"
                )
            )
            add(
                Model(
                    id = "Qwen2.5-7B-Instruct-Q4_K_M.gguf",
                    name = "Qwen 2.5 7B Q4_K_M",
                    size = 4500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF",
                    description = "Flagship Qwen 2.5 model"
                )
            )

            // LLAMA FAMILY (GGUF)
            add(
                Model(
                    id = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                    name = "Llama 3.2 1B Q4_K_M",
                    size = 800,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF",
                    description = "Meta's efficient 1B model"
                )
            )
            add(
                Model(
                    id = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                    name = "Llama 3.2 3B Q4_K_M",
                    size = 2000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF",
                    description = "Balanced Llama model"
                )
            )
            add(
                Model(
                    id = "Llama-3.3-8B-Instruct-Q4_K_M.gguf",
                    name = "Llama 3.3 8B Q4_K_M",
                    size = 5000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/Llama-3.3-8B-Instruct-GGUF",
                    description = "Latest Llama with improved capabilities"
                )
            )

            // MICROSOFT PHI FAMILY (GGUF)
            add(
                Model(
                    id = "phi-2-Q4_0.gguf",
                    name = "Phi-2 Q4_0",
                    size = 1500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/TheBloke/phi-2-GGUF",
                    description = "2.7B reasoning specialist"
                )
            )
            add(
                Model(
                    id = "phi-3-mini-4k-instruct-Q4_K_M.gguf",
                    name = "Phi-3 Mini Q4_K_M",
                    size = 2300,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/Phi-3-mini-4k-instruct-GGUF",
                    description = "Phi-3 with 4K context"
                )
            )
            add(
                Model(
                    id = "Phi-4-mini-instruct-Q4_K_M.gguf",
                    name = "Phi-4 Mini Q4_K_M",
                    size = 3600,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/Phi-4-mini-instruct-GGUF",
                    description = "Latest Phi with enhanced reasoning"
                )
            )

            // DEEPSEEK FAMILY (GGUF)
            add(
                Model(
                    id = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
                    name = "DeepSeek R1 Distill 1.5B",
                    size = 1000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
                    description = "Chain-of-thought reasoning model"
                )
            )
            add(
                Model(
                    id = "deepseek-coder-1.3b-instruct-Q4_K_M.gguf",
                    name = "DeepSeek Coder 1.3B",
                    size = 900,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/TheBloke/deepseek-coder-1.3b-instruct-GGUF",
                    description = "Specialized coding model"
                )
            )

            // MISTRAL FAMILY (GGUF)
            add(
                Model(
                    id = "Mistral-7B-Instruct-v0.3-Q4_K_M.gguf",
                    name = "Mistral 7B v0.3 Q4_K_M",
                    size = 4500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF",
                    description = "Popular general-purpose model"
                )
            )

            // TINYLLAMA FAMILY (GGUF)
            add(
                Model(
                    id = "TinyLlama-1.1B-Chat-v1.0-Q4_K_M.gguf",
                    name = "TinyLlama 1.1B Q4_K_M",
                    size = 700,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
                    description = "Fast, compact chat model"
                )
            )

            // SMOLLM FAMILY (GGUF)
            add(
                Model(
                    id = "SmolLM-135M-Instruct-Q4_K_M.gguf",
                    name = "SmolLM 135M Q4_K_M",
                    size = 100,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-135M-Instruct-GGUF",
                    description = "Smallest viable instruction model"
                )
            )
            add(
                Model(
                    id = "SmolLM-360M-Instruct-Q4_K_M.gguf",
                    name = "SmolLM 360M Q4_K_M",
                    size = 250,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-360M-Instruct-GGUF",
                    description = "Better quality ultra-compact model"
                )
            )

            // STABLE LM (GGUF)
            add(
                Model(
                    id = "stablelm-2-zephyr-1_6b-Q4_K_M.gguf",
                    name = "StableLM 2 Zephyr 1.6B",
                    size = 1000,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/second-state/stablelm-2-zephyr-1.6b-GGUF",
                    description = "DPO-tuned conversational model"
                )
            )

            // SPECIALIZED MODELS
            add(
                Model(
                    id = "LFM2.5-1.2B-Thinking-Q8_0.gguf",
                    name = "LFM 2.5 1.2B Thinking",
                    size = 1500,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/bartowski/LFM-2.5-1.2B-Thinking-GGUF",
                    description = "Thinking model variant"
                )
            )
            add(
                Model(
                    id = "Gemini-Nano-Gemmafied.Q3_K_M.gguf",
                    name = "Gemini-Nano Q3_K_M",
                    size = 1600,
                    modelType = modelType,
                    downloadUrl = "https://huggingface.co/lmstudio-community/Gemini-Nano-Gemmafied-GGUF",
                    description = "Gemini Nano converted to GGUF"
                )
            )
        }


        /**
         * Get all multimodal (vision-enabled) models
         */
        fun getMultimodalModels(): List<Model> {
            return (MEDIA_PIPE_MODELS + LITE_RT_LM_MODELS).filter {
                it.features.containsAll(
                    listOf(
                        ModelManagerRuntimeFeature.TEXT,
                        ModelManagerRuntimeFeature.AUDIO,
                        ModelManagerRuntimeFeature.VISION
                    ),
                )
            }
        }

        /**
         * Get recommended models for different use cases
         */
        object Recommendations {
            // Best overall balance of size and quality
            val BEST_OVERALL = listOf(
                "Qwen2.5-1.5B-Instruct",
                "Llama-3.2-1B-Instruct",
                "gemma3-1B-it"
            )

            // Smallest models for constrained devices
            val ULTRA_COMPACT = listOf(
                "SmolLM-135M-Instruct",
                "gemma-3-270m-it",
                "Qwen2.5-0.5B-Instruct"
            )

            // Best for vision/multimodal tasks
            val MULTIMODAL = listOf(
                "gemma-3n-E4B-it", // Best quality
                "gemma-3n-E2B-it", // Better efficiency
                "FastVLM-0.5B"     // Fastest
            )

            // Best for coding
            val CODING = listOf(
                "Qwen2.5-3B-Instruct",
                "DeepSeek-R1-Distill-Qwen-1.5B",
                "Phi-4-mini-instruct"
            )

            // Best for reasoning
            val REASONING = listOf(
                "Phi-4-mini-instruct",
                "DeepSeek-R1-Distill-Qwen-1.5B",
                "Qwen3-4B"
            )

            // Best for multilingual
            val MULTILINGUAL = listOf(
                "Qwen2.5-1.5B-Instruct",
                "Qwen2.5-3B-Instruct",
                "Qwen3-4B"
            )
        }

        /**
         * IMPORTANT DOWNLOAD NOTES:
         *
         * MediaPipe (.task (Recommended) and .litertlm) Models:
         * - Download from HuggingFace litert-community
         * - Files are pre-bundled and ready to use
         * - No conversion needed
         *
         * LiteRT-LM (.litertlm) Models:
         * - Download from HuggingFace with library:litert-lm filter
         * - Next-gen format with better compression
         * - Some models have separate Web and Android variants
         *
         * LlamaTIK (.gguf) Models:
         * - Download any GGUF model from HuggingFace
         * - Look for Q4_K_M quantization for best balance
         * - Q8_0 for higher quality but larger size
         * - Q3_K_M for extreme size constraints
         *
         * Quantization Guide:
         * - Q2: Extreme compression, noticeable quality loss
         * - Q3: Good compression, acceptable quality
         * - Q4: Best balance (recommended)
         * - Q5/Q6: Higher quality, larger size
         * - Q8: Near-original quality
         * - F16/F32: Full precision (rarely needed)
         **/

        private val ALL_MODELS_ANDROID = LITE_RT_LM_MODELS + MEDIA_PIPE_MODELS + LlAMATIK_MODELS
        private val ALL_MODELS_IOS = MEDIA_PIPE_MODELS_IOS
    }
}

enum class ModelSupportedFormat(
    val format: String,
) {
    GGUF("gguf"), BIN("bin"), LITERT("litertlm"), TASK("task")
}

data class EmbeddingModel(
    val id: String,
    val name: String,
    val downloadUrl: String,
    val description: String,
    val localPath: String? = null,
) {

    companion object {

        val EMBEDDING_MODELS = buildList {
            add(
                EmbeddingModel(
                    id = "embeddinggemma-300M_seq256_mixed-precision.tflite",
                    name = "Gemma Embedding 300M",
                    downloadUrl = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma_embedding/float16/1/gemma_embedding.tflite", // Example link, needs verification
                    description = "Official MediaPipe Gemma Embedding Model"
                )
            )
            add(
                EmbeddingModel(
                    id = "google-bert-base-uncased.tflite",
                    name = "BERT Base Uncased",
                    downloadUrl = "https://tfhub.dev/tensorflow/lite-model/mobilebert/1/default/1?lite-format=tflite",
                    description = "General purpose embedding model"
                )
            )
        }
    }

}

data class TokenizerModel(
    val id: String,
    val name: String,
    val downloadUrl: String,
    val description: String,
    val localPath: String? = null,
) {

    companion object Companion {

        val TOKENIZER_MODELS = buildList {
            add(
                TokenizerModel(
                    id = "sentencepiece.model",
                    name = "Gemma SentencePiece",
                    downloadUrl = "https://huggingface.co/google/gemma-2b/resolve/main/tokenizer.model",
                    description = "Standard tokenizer for Gemma models"
                )
            )
        }
    }

}