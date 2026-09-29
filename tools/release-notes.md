# 🚀 DRS AI APP v1.1.0 — الإصدار الأول | First Public Release

**AI ON DEVICE — PRIVATE — OFFLINE — NO CLOUD REQUIRED**

---

## 🇸🇦 بالعربية

أول إصدار عام من محرك ذكاء اصطناعي أندرويد **حقيقي وقابل للتثبيت** يعمل بالكامل على الجهاز.

### ما الجديد في v1.1.0
- **KV-cache تزايدي**: يُرمّز سجل المحادثة مرة واحدة فقط وتُعاد استخدام البادئة المخزّنة في كل دورة — بدل إعادة ترميز التاريخ كاملاً في كل رسالة (أسرع بكثير في المحادثات الطويلة)
- محرك llama.cpp b6000 مدمج أصلياً (NDK/JNI، arm64-v8a)
- صدق كامل: حالة «مطلوب نموذج» واضحة حتى تستورد نموذج GGUF — ولا ادعاء بقدرات غير موجودة

### الميزات
| | |
|---|---|
| 💬 محادثة محلية بالبث | مع حارس سياق وتسلسلات إيقاف |
| 📚 RAG كامل | استيراد مستندات ← تقطيع ← تضمين محلي ← بحث شعاعي |
| 👁 رؤية | فهم الصور عبر mtmd الأصلي |
| 🎙 صوت | تفريغ نصي عبر whisper.cpp v1.7.4 |
| 🧰 7 أدوات | حاسبة، تحويل وحدات ودرجات، وقت/تاريخ، معلومات الجهاز، regex، Base64، SHA-256 |
| 🔒 خصوصية | قفل تطبيق (PBKDF2-150k)، صفر بيانات تخرج من الجهاز |
| 🌐 ثنائي اللغة | عربي/إنجليزي مع RTL كامل |
| 📤 تصدير | JSON / Markdown / TXT / PDF |

### التثبيت (Android 8.0+، معالج ARM64)
1. نزّل **`DRS-AI-v1.1.0-arm64-release.apk`** (14 MB موقّع)
2. ثبّته (اسمح بـ «مصادر غير معروفة» إن طُلب)
3. افتح التطبيق ← **مركز النماذج** ← استورد نموذج GGUF
   - الموصى به: `Qwen2.5-1.5B-Instruct-Q4_K_M` أو `Llama-3.2-1B-Instruct-Q4_K_M`
4. جرّب: محادثة، أسئلة عن مستنداتك، تحليل صور، تفريغ صوتي — كله دون إنترنت

### حدود صادقة (بشفافية كاملة)
- الاستدلال على CPU فقط في هذا الإصدار — **Vulkan مكتشف لكنه غير مستخدم**، والتطبيق يبلّغ عن ذلك بصدق (تفعيل GPU الحقيقي في خريطة التطوير v1.2)
- لا نماذج مضمّنة في الـ APK (حجمها 1–4 GB) — تستوردها بنفسك
- اختبارات الأجهزة الحقيقية (instrumented) تحتاج هاتفاً فعلياً

### التحقق
- APK MD5: `c1e882c6fb12418d302e0b74083fa313`
- 22/22 فحص دخان على الحاسوب (منها التحقق من إعادة استخدام KV-cache)
- 23/23 اختبار وحدة على JVM
- توقيع RSA-2048 صالح 30 سنة (apksigner v2/v3)

---

## 🇬🇧 English

First public release of a **truly installable, honest, fully offline** Android AI engine.

### New in v1.1.0
- **Incremental KV-cache**: conversation history is encoded once; each new turn reuses the cached
  prefix instead of re-encoding the full history (much faster in long conversations)
- Native llama.cpp b6000 integration (NDK/JNI, arm64-v8a)
- Radical honesty: clear "Model Required" gating until a GGUF model is imported

### Honest limits (full transparency)
- CPU-only inference in this release — Vulkan is detected but **not used** (real GPU acceleration is planned for v1.2, see ROADMAP.md)
- No bundled models (they are 1–4 GB) — import your own GGUF via Model Center
- Instrumented tests require a physical device

### Verify
- APK MD5: `c1e882c6fb12418d302e0b74083fa313`
- 22/22 host smoke checks (incl. KV-cache REUSE verification) · 23/23 JVM unit tests

> 🗺 خريطة التطوير الكاملة في [ROADMAP.md](../../blob/main/ROADMAP.md) — Vulkan، armeabi-v7a، صوت متدفق، وضع الوكيل، v2.0
