// v1.4.0 — dependency refresh (latest stable, Maven-verified 2026-09):
// v1.4.0 — AGP 9.4.1 (built-in Kotlin — required by the newest AndroidX line) · Gradle 9.6.0 ·
// Compose/serialization plugin pinned to the embedded Kotlin 2.2.10 · KSP 2.3.12 (KSP2)
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
