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

---

# ⚡ DRS AI APP v1.4.0 — تبعيات 2026 + تفاعل أذكى | 2026 Toolchain + Smarter Interaction

## 🇸🇦 بالعربية

### الجديد في v1.4.0
- **كل التبعيات لأحدث إصدار مستقر** (تحقق Maven 2026-09):
  AGP 8.5.2 → **9.4.1** (Kotlin 2.2.10 مدمج) · Gradle 8.9 → **9.6.0** · compileSdk 34 → **37** ·
  targetSdk → 36 · Compose BOM 2024.09 → **2026.09.00** (Compose 1.12.1 / Material3 1.5.x) ·
  Room 2.6.1 → **2.8.5** · Navigation 2.8.2 → **2.10.2** · core-ktx **1.19.1** · lifecycle **2.11.0** ·
  DataStore **1.2.1** · serialization **1.11.0** · coroutines **1.11.0** · jsoup **1.23.2** · KSP **2.3.12**
  — بلا أذونات جديدة، وبنفس حجم APK التقريبي.
- **ضغط السياق التدريجي**: المحادثات الطويلة لم تعد تُقص — تلخيص محلي للأدوار القديمة في ملخص جارٍ
  يُحفظ داخل صف الجلسة، مع **مؤشر صادق** في الشاشة يوضح ما يراه النموذج فعلياً.
- **مدير الجلسات**: بحث / تثبيت / إعادة تسمية / حذف من مربع واحد.
- **مكتبة قوالب الأوامر**: حفظ وتصنيف وإعادة استخدام مع عدّاد استخدام — محلي 100%.
- **تفاعل «Nova 2.0»**: ثيم AMOLED أسود حقيقي، شريط سفلي بحركة ربيعية (حبة منزلقة + تكبير)،
  انتقالات شاشات آمنة RTL، مؤشر كتابة بنقاط تفكير، شارة سرعة لكل رسالة (رمز/ثانية)، نص بثّ متحرك،
  بطاقة إحصاءات استخدام محلية في لوحة التحكم.
- **معالج ترحيب لأول تشغيل** — مرة واحدة، قابل للتخطي، يوجّه لاستيراد النموذج.
- **ترحيل Room 2 → 3 غير مدمّر** — كل المحادثات والنماذج والذكريات محفوظة.

## 🇬🇧 English

### New in v1.4.0
- **Every dependency on the latest stable line (2026-09, Maven-verified)** — AGP 9.4.1 (built-in
  Kotlin 2.2.10), Gradle 9.6.0, compileSdk 37 / targetSdk 36, Compose BOM 2026.09.00,
  Room 2.8.5, Navigation 2.10.2, core-ktx 1.19.1, lifecycle 2.11.0, DataStore 1.2.1,
  serialization/coroutines 1.11.0, jsoup 1.23.2, KSP 2.3.12 — zero new permissions.
- **Smart context compression** — long chats are summarized locally into a rolling session summary
  (replaces hard trimming) with an honest on-screen indicator of what the model sees.
- **Sessions manager** — search / pin / rename / delete in one dialog.
- **Prompt template library** — save, categorize, reuse, with usage counts; fully local.
- **"Nova 2.0" interactions** — true-black AMOLED theme, spring-animated bottom bar (sliding pill +
  scale), RTL-safe transitions, thinking-dots typing indicator, per-message speed badge (tok/s),
  animated streaming text, local usage-stats card.
- **First-run onboarding wizard** — skippable, guides model import.
- **Non-destructive Room migration (2 → 3)** — all data preserved.

### Verify
- Signature identical to v1.1.0–v1.3.0 (certificate SHA-256 `1fd35c8e…`) — direct in-place upgrade.
