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

## Vector database

This project uses [sqlite-vec](https://github.com/asg017/sqlite-vec) (v0.1.6) as a SQLite extension to enable on-device vector similarity search for RAG workflows.

### What it does

- Stores document chunk embeddings in a `vec0` virtual table (`doc_embeddings`)
- Performs KNN search using sqlite-vec's `MATCH` operator to retrieve the most semantically relevant chunks for a given query embedding
- Embedding dimension: `float[512]`
- Returns top-6 nearest neighbors ordered by distance

### Platform integration

| Platform | Mechanism | Native artifact |
|----------|-----------|-----------------|
| Android  | JNI extension loaded via `BundledSQLiteDriver.addExtension("sqlitevec", "sqlite3_vec_init")` | `sqlitevec.so` (arm64-v8a, armeabi-v7a, x86_64) |
| iOS      | Kotlin/Native cinterop + prebuilt static libraries linked at compile time | `libsqlitevec.a` (arm64, arm64-simulator, x64-simulator) |

- **Android:** `.so` files are placed under `jniLibs/` and loaded automatically at runtime.
- **iOS:** `libsqlitevec.a` static libraries are linked via cinterop definitions in `build.gradle.kts`; `register_sqlite_vec()` is called before the `NativeSQLiteDriver` is created.

## Local database (Room)

This project uses [Room](https://developer.android.com/training/data-storage/room) (v2.8.4) as the KMP-compatible SQLite ORM for structured local persistence.

### Schema

| Entity | Purpose |
|--------|---------|
| `ChatEntity` | Stores chat sessions |
| `MessageEntity` | Stores individual messages per chat, including attachment metadata (serialised via `AttachmentListConverter`) |

The `VectorEmbeddingsDao` provides raw SQL access to the sqlite-vec `doc_embeddings` virtual table alongside the standard Room DAOs (`ChatDao`, `MessageDao`).

### Platform setup

Room is initialised with `@ConstructedBy(RoomDBConstructor::class)` for KMP compatibility. Each platform supplies its own `DatabaseBuilder`:

| Platform | SQLite driver |
|----------|--------------|
| Android  | `BundledSQLiteDriver` (androidx.sqlite) |
| iOS      | `NativeSQLiteDriver` (SQLite.swift system SQLite) |

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
- sqlite-vec (on-device vector similarity search)
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
