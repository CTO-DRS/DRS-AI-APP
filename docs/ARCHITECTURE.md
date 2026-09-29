# DRS AI — Architecture

## Overview

DRS AI is an offline-first Android AI platform. Every AI feature executes on the
device through native engines; the only network use is an optional, user-gated,
one-time model download.

```
┌─────────────────────────────────────────────────────┐
│  features/ (Compose screens + ChatViewModel)        │
│  dashboard · chat · models · documents · vision     │
│  voice · tools · settings · privacy · diagnostics   │
├─────────────────────────────────────────────────────┤
│  core/ (engine-agnostic contracts)                  │
│  ai/LlmEngine · inference/LlamaCppEngine            │
│  models/GgufParser+HardwareProfile+ModelRepository  │
│  memory · rag · vision · voice · tools · diagnostics│
├─────────────────────────────────────────────────────┤
│  data/ (Room DB · DataStore settings · SAF I/O)     │
├─────────────────────────────────────────────────────┤
│  native/ (C++17, NDK r27c, arm64-v8a)               │
│  drs_core.cpp (portable core over llama.cpp)        │
│  drs_jni.cpp · drs_vision_jni.cpp · drs_whisper_jni │
└─────────────────────────────────────────────────────┘
```

## Native layer

- **Engine choice**: llama.cpp (pinned b6000) — mature GGUF support, sampler
  chains, embeddings, and the mtmd multimodal module; whisper.cpp v1.7.4 for STT.
- **drs_core** wraps llama.cpp behind a portable C API-like class:
  - load / unload with explicit resource lifecycle
  - two-pass tokenization sizing
  - **incremental KV-cache**: longest common prefix with the cached prompt is
    reused (`llama_memory_seq_rm` at the divergence point); unrelated prompts
    fall back to a full re-encode; identical prompts force re-eval of the final
    token. Statistics (prefix hits, full re-encodes) are exposed to Kotlin.
  - stop-sequence holdback: a tail up to the longest stop marker is withheld
    from the stream until it cannot be part of a marker
  - honest context guard: oversized prompts are rejected with a clear message
  - embeddings via mean pooling (`llama_get_embeddings_seq`), L2-normalized
- **Symbol isolation**: whisper + its ggml are statically linked into
  `libdrs_whisper_jni.so` with `-fvisibility=hidden`, preventing runtime
  interposition with llama's shared ggml.
- 8 stripped shared libraries are packaged into `jniLibs/arm64-v8a`.

## Kotlin layer

- Single Gradle module with strict package layering (`core/data/domain/ui/features`).
- Manual DI (`AppContainer`) — no reflection, plugin-ready interfaces.
- Room database: sessions, messages, models, memories, documents, vector rows.
- DataStore settings: appearance, language, engine params, privacy switches.
- RAG: local chunker (sliding window, sentence snapping) → embedder engine →
  little-endian float BLOBs in Room → brute-force cosine retrieval (sized for
  on-device corpora).
- Honest UI states: every feature that needs a missing model shows a real
  "Model Required" message explaining exactly which file is missing.

## Device profiling

`HardwareProfiler` classifies devices (ENTRY/BALANCED/PERFORMANCE/EXTREME) from
real RAM, ABI and core count, and derives recommended threads, context and RAM
budgets used by compat checks and defaults.
