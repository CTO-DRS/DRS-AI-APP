# DRS AI — Model guide

DRS AI ships without models (honest ~16 MB APK) and imports GGUF files.
Everything runs on ARM64 CPU.

## Where to get models

Any GGUF chat model works. Recommended starting points (Hugging Face, GGUF):

| Use case | Model | Size | Notes |
|----------|-------|------|-------|
| Chat (balanced) | Qwen2.5 1.5B Instruct Q4_K_M | ~1.0 GB | Best small all-rounder |
| Chat (entry) | Llama 3.2 1B Instruct Q4_K_M | ~0.8 GB | Fast on low-RAM phones |
| Chat (strong) | Qwen2.5 3B Instruct Q4_K_M | ~2.0 GB | Needs PERFORMANCE profile |
| Embedder (RAG) | bge-m3 Q8_0 / nomic-embed-text-v1.5 Q8_0 | ~0.7/0.1 GB | any embeddings-capable GGUF |
| Vision chat | Gemma 3 4B IT Q4_K_M (+ mmproj) / MiniCPM-V 2.6 (+ mmproj) | ~2.5 GB + proj | import both files |
| STT | whisper tiny / base / small GGML | 75–500 MB | whisper.cpp GGML format |

## Import flow

Models tab → Import → pick file → choose its kind:

- **Chat** — generative GGUF (validated: GGUF magic + non-clip architecture)
- **Embedder** — embedding GGUF used by the RAG pipeline
- **Vision (mmproj)** — projector GGUF (validated: clip architecture)
- **Whisper (GGML)** — whisper.cpp GGML (validated: ggml magic)

Files are copied into app-private storage; the original stays untouched.

## Compatibility checks

Per import, DRS AI verifies against your real hardware: ABI, Android version,
storage, RAM working-set vs budget, context length vs device max, chat
readiness and quantization friendliness — each item is PASS / WARN / FAIL with
an honest note (e.g. "prefer 0.5–1.5B Q4 models, context ≤ 2048, 2 threads").

## Runtime defaults by device profile

| Profile | RAM | Max ctx | Threads | Batch |
|---------|-----|---------|---------|-------|
| Entry | < 4 GB | 2048 | 2 | 128 |
| Balanced | 4–8 GB | 4096 | 2–4 | 256 |
| Performance | 8–16 GB | 8192 | 4–6 | 256 |
| Extreme | 16+ GB | 16384 | 6–8 | 512 |

## Tips

- Q4_K_M / Q5_K_M are the mobile sweet spot.
- Keep runtime context ≤ the device max shown in Dashboard/Diagnostics.
- The embedder and the chat model load independently; you can run RAG with a
  small embedder while chatting with a larger model.
