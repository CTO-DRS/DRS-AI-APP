# DRS AI R8 rules
-keep class com.drs.ai.core.inference.LlamaNative { *; }
-keep class com.drs.ai.core.vision.VisionNative { *; }
-keep class com.drs.ai.core.voice.WhisperNative { *; }
-keep class com.tom.roush.pdfbox.** { *; }
-dontwarn com.tom.roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.drs.ai.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
