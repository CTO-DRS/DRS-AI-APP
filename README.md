# DRS AI APP — Offline Android AI Engine | محرّك ذكاء اصطناعي أندرويد يعمل دون إنترنت

**AI ON DEVICE — PRIVATE — OFFLINE — NO CLOUD REQUIRED**

A truly installable, honest, fully offline Android AI application powered by native `llama.cpp` (b6000).
No WebView fake shells. No cloud calls. No simulated features. When a model is missing, the app tells you
honestly: **"Model Required"** — and guides you to import one.

تطبيق أندرويد حقيقي قابل للتثبيت، يعمل بالكامل على الجهاز دون أي اتصال سحابي، مدعوم بمحرّك
`llama.cpp` الأصلي عبر NDK/JNI. بلا حيل WebView، بلا مكالمات سحابية، بلا ميزات وهمية.

---

## ✨ Features | الميزات

| Feature | الوصف |
|---|---|
| 💬 **Chat** | Local streaming chat with context guard and stop-sequence support |
| 📚 **RAG** | Import documents → chunk → local embeddings → vector search → grounded answers |
| 👁 **Vision** | Image understanding via native `mtmd` (llama.cpp multimodal) |
| 🎙 **Voice** | Speech-to-text via native `whisper.cpp` v1.7.4 |
| 🧰 **7 Tools** | Calculator, unit/temperature converter, datetime, device info, regex, Base64, SHA-256 |
| 🔒 **Privacy** | App lock (PBKDF2-150k), everything stays on device, zero telemetry |
| 🌐 **Bilingual** | Full Arabic/English UI with proper RTL |
| 📤 **Export** | Conversations to JSON / Markdown / TXT / PDF |

## 📦 Install | التثبيت

1. Download `DRS-AI-v1.2.1-arm64-release.apk` from [Releases](../../releases/tag/v1.2.1) (14 MB, signed, R8-minified)
2. Install on Android 8.0+ (ARM64) — allow "unknown sources" if asked
3. Download a ready model **from the release pages** — no external site needed | نزّل نموذجاً جاهزاً من صفحات الإصدارات:

   | Model | Size | Best for | الأفضل لـ |
   |---|---|---|---|
   | [Qwen2.5-1.5B-Instruct-Q4_K_M.gguf](../../releases/download/v1.1.0/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf) ⭐ | 1066 MB | Best overall — strong **Arabic** + English, math & reasoning | جودة عالية ودعم عربي ممتاز |
   | [Llama-3.2-1B-Instruct-Q4_K_M.gguf](../../releases/download/v1.1.0/Llama-3.2-1B-Instruct-Q4_K_M.gguf) | 770 MB | Lighter on RAM, English-focused | أخف على الذاكرة |
   | [Qwen2.5-0.5B-Instruct-Q4_K_M.gguf](../../releases/download/v1.2.0/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf) | 469 MB | Entry-level phones (2–3 GB RAM) | للأجهزة الضعيفة — سريع جداً |
   | [SmolVLM2-500M Q8_0 + mmproj](../../releases/tag/v1.2.0) | 417 + 104 MB | **Vision** — import both files (model + projector) | الرؤية — استورد الملفين معاً |

4. Open the app → **Model Center** → import the GGUF file | افتح التطبيق ← مركز النماذج ← استورد ملف GGUF
5. Chat, ask questions about your documents, analyze images, transcribe voice — all offline

> ⚠️ **Honest design**: the APK ships **without** bundled models (models are 0.4–1.1 GB each). The app shows
> clear "Model Required" states until you import one. This is a feature, not a bug.
> التصميم الصادق: التطبيق يأتي بلا نماذج مدمجة، ويعرض حالة «مطلوب نموذج» بوضوح حتى تستورد واحداً.

## 🎨 What's new in v1.2.1 | جديد الإصدار

- **Full-surface UI overhaul completed** — every remaining screen rebuilt with the modern shell:
  Model Center (tinted model cards + honest import flow), Documents (search + doc cards), Settings
  (sectioned cards with pill sliders + switch rows), Vision (requirement banners + full-bleed image),
  Voice, Tools (scrollable chip tabs), Diagnostics, About, Privacy, and a redesigned Lock screen
- New shared component kit: `GradientBanner`, `SectionCard`, `IconBadge`, `SliderRow`, `SwitchRow`,
  `EmptyState`, `PrimaryAction`, `SoftAction`, `ChipRow`, and soft entrance animations
- Same signature key, same honest engine — drop-in upgrade over v1.2.0
- إكمال تحديث كل الشاشات المتبقية بهوية Nova Indigo مع مكونات مشتركة جديدة وحركات دخول ناعمة

## 🎨 What's new in v1.2.0 | جديد الإصدار

- **"Nova Indigo" redesign** — indigo→violet→cyan brand gradient, refined dark mode, softer shapes,
  tactile press animations across every screen
- **New smart-robot app icon** (adaptive + legacy, all densities)
- **Dashboard**: gradient hero card, pulsing generation status, tinted quick-action grid
- **Chat**: modern rounded bubbles + pill input bar with circular send/stop buttons
- **SmolVLM2 vision bundle** hosted on the release for one-page setup
- إعادة تصميم شاملة بهوية عصرية، أيقونة روبوت ذكي، ودعم رؤية جاهز للتنزيل من صفحة الإصدار

## 🧠 Models | النماذج

- **Qwen2.5-1.5B-Instruct (Q4_K_M)** — official Qwen quantization. Recommended default; best multilingual
  quality including Arabic, 16K context, ~1.1 GB RAM at inference.
- **Llama-3.2-1B-Instruct (Q4_K_M)** — Meta's compact instruct model; lower RAM footprint, great English.
- Any other GGUF (`Q4_K_M`/`Q5_K_M`/`Q6_K`, ≤ 4B recommended for phones) can be imported via SAF.
- SHA-256 checksums for the hosted models are listed in the release notes — verify after download.
- نماذج GGUF أخرى متوافقة يمكن استيرادها مباشرة من وحدة تخزين الجهاز عبر SAF.

## 🏗 Architecture | البنية

- **Engine**: llama.cpp b6000 (pinned) + whisper.cpp v1.7.4, built with NDK r27c for `arm64-v8a`
- **Native libs (10)**: `libllama`, `libggml`, `libggml-cpu`, `libggml-base`, `libmtmd`, `libdrs_core`, `libdrs_vision`, `libdrs_whisper_jni`, …
- **v1.1.0 highlight**: **incremental KV-cache** — conversation history is encoded once; new turns
  reuse the cached prefix (`llama_memory_seq_rm`) instead of re-encoding the full history every turn
- **v1.2.0 highlight**: full **"Nova Indigo" UI redesign** + new robot icon — same honest engine underneath
- **v1.2.1 highlight**: **every screen modernized** — shared component kit, sectioned settings, tinted
  model cards, soft entrance animations (23/23 JVM tests green)
- **App**: Kotlin + Jetpack Compose (M3), Room, DataStore, SAF import, single-module clean layering
- **Honest diagnostics**: the app reports CPU-only inference truthfully (Vulkan backend detected but
  not used in this release — see roadmap)

## ✅ Quality Gates | بوابات الجودة

- 22/22 host smoke-test checks (including KV-cache REUSE verification)
- 23/23 JVM unit tests (chunker, vectors, tools, GGUF parser, crypto vectors)
- Release APK: signed (RSA-2048, 30y), apksigner-verified (v2/v3 schemes)

## 🗺 Roadmap | خريطة التطوير

See [ROADMAP.md](ROADMAP.md) — GPU (Vulkan) inference, armeabi-v7a, streaming ASR, agent mode…

## 📄 License | الترخيص

MIT — see [LICENSE](LICENSE)
