# DRS AI — Building from source

## Requirements

- JDK 17 (AGP 8.5.2)
- Android SDK: platform-34, build-tools 34.0.0, platform-tools, CMake 3.22.1
- Android NDK r27c (27.2.12479018)
- 2+ GB free disk for toolchains and intermediates

## 1. Native libraries (arm64-v8a)

Third-party engines are vendored under `native/third_party/`:

```bash
# llama.cpp pinned tag b6000, whisper.cpp pinned tag v1.7.4
bash native/build_android.sh
```

Outputs 8 stripped `.so` files into `app/src/main/jniLibs/arm64-v8a/`:
`libllama.so`, `libggml.so`, `libggml-base.so`, `libggml-cpu.so`, `libmtmd.so`,
`libdrs_core_jni.so`, `libdrs_vision_jni.so`, `libdrs_whisper_jni.so`.

### Host smoke test (recommended before packaging)

```bash
bash native/build_host.sh
./native/build-host/smoke_test <tiny-model.gguf>   # e.g. stories260K-f32.gguf
```

The smoke test covers: load, tokenize, generation, prefix-cache reuse,
full-re-encode fallback, stop sequences with holdback, sink-driven stop,
context guard, unload/reload. All 22 checks must pass.

## 2. Android app

```bash
export ANDROID_HOME=/path/to/sdk
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # 23 JVM tests (chunker, vectors, tools, GGUF parser)
./gradlew assembleRelease        # R8-minified, signed with app/drs-release.keystore
```

Release signing uses `app/drs-release.keystore`
(store/key password: `drs-ai-release-2026`, alias `drsai`) — replace it with
your own keystore for distribution.

## 3. Instrumented tests (physical device)

```bash
./gradlew connectedDebugAndroidTest
```

`NativeInstrumentedTest` verifies DB creation, native library loading and
engine handle lifecycle on real hardware.

## 4. Manual test matrix (16 scenarios)

1. Fresh install → Dashboard shows LOCAL/OFFLINE + "no model" honest state
2. Import non-GGUF file as chat → clear rejection (bad magic)
3. Import valid chat GGUF → header parsed (arch/quant/ctx shown)
4. Compat report on low-RAM device → WARN/FAIL notes with downgrade advice
5. Chat without model → honest "Model Required" redirect
6. Chat with model → streaming tokens appear, DB persists across restart
7. Stop mid-generation → partial answer kept, no crash
8. Regenerate → previous answer replaced, output differs (seeded randomness)
9. Second turn → faster prefill (prefix cache reuse), coherent context
10. Context overflow → oldest trimmed + honest notice shown
11. RAG import without embedder → honest "Embedder model required"
12. RAG import with embedder (e.g. bge-m3) → chunks embedded, semantic search works
13. Vision without mmproj → honest 2-file requirement message
14. Vision with chat+mmproj → streamed description of picked image
15. STT without whisper model → honest requirement; with model → transcript
16. Airplane mode full pass: chat/RAG/vision/STT all function with zero network
