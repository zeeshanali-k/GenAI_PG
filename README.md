# GenAI_PG

On-device GenAI playground built with Kotlin Multiplatform (Android + iOS), focused on testing
multiple local LLM runtimes and lightweight RAG workflows.

> [!WARNING]
> This project is a **playground/experimental repo** and is **not production-ready**.
> It is intended for learning, prototyping, and runtime comparisons. APIs, model compatibility, and
> behavior can change at any time.

## What this repo does

- Runs local LLM inference on-device.
- Lets you switch between different runtime backends.
- Supports basic chat workflows, document attachments, and RAG-style context retrieval.
- Shares most logic in KMP (`composeApp`) with Android and iOS host apps.

## AI libraries used

- **MediaPipe (GenAI / LLM Inference)**
    -
    Docs: [MediaPipe LLM Inference](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference)
    - Android artifact used: `com.google.mediapipe:tasks-genai`
- **LiteRT-LM**
    - LiteRT overview: [Google AI Edge LiteRT](https://ai.google.dev/edge/litert)
    - Source: [LiteRT-LM (GitHub)](https://github.com/google-ai-edge/LiteRT/tree/main/litertlm)
    - Android artifact used: `com.google.ai.edge.litertlm:litertlm-android`
- **Llamatik**
    - Docs: [Llamatik Documentation](https://docs.llamatik.com/)
    - Source: [Llamatik (GitHub)](https://github.com/ferranpons/llamatik)
    - Artifact used: `com.llamatik:library`

Additional AI-related dependency in this repo:

- `com.google.ai.edge.localagents:localagents-rag`

## Runtime support in this project

| Runtime   | Android | iOS                                     | Notes                                        |
|-----------|---------|-----------------------------------------|----------------------------------------------|
| MediaPipe | Yes     | Yes                                     | Main cross-platform runtime in this repo     |
| LiteRT-LM | Yes     | No (not enabled in current app options) | Android-only in current UI/runtime selection |
| Llamatik  | Yes     | Yes                                     | Used for GGUF-based flow                     |

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform UI
- Koin (DI)
- Room + SQLite (local persistence)
- MOKO Permissions

## Project structure

- `app/` - Android application shell
- `GenAI_PG_KMP/composeApp/` - Shared KMP code (UI, domain, runtime managers, RAG, DB)
- `GenAI_PG_KMP/iosApp/` - iOS host app
- `media/` - Demo videos and recordings

## Demo videos

### Android demo

<video src="./media/android.mov" controls width="720">
  Your browser does not support the video tag.
</video>

Fallback link: [android.mov](./media/android.mov)

### iOS demo

<video src="./media/iOS.MP4" controls width="720">
  Your browser does not support the video tag.
</video>

Fallback link: [iOS.MP4](./media/iOS.MP4)

## Notes

- Models are large; expect significant download/storage usage.
- Performance depends heavily on device hardware, model size, quantization, and backend.
- This repo is intentionally optimized for experimentation over production hardening.
