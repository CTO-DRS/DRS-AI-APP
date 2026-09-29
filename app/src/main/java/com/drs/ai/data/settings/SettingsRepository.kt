package com.drs.ai.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "drs_settings")

data class GenParams(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val minP: Float = 0.05f,
    val repeatPenalty: Float = 1.1f,
    val maxTokens: Int = 512,
    val seed: Long = -1L
)

data class AppSettings(
    val themeMode: Int = 0, // 0 system, 1 light, 2 dark
    val dynamicColors: Boolean = true,
    val language: String = "system", // system | en | ar
    val threads: Int = 0, // 0 = auto
    val contextSize: Int = 2048,
    val batchSize: Int = 256,
    val gpuLayers: Int = 0,
    val flashAttention: Boolean = false,
    val maxRamPct: Int = 60,
    val autoUnload: Boolean = true,
    val allowNetwork: Boolean = false,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val systemPrompt: String = "",
    val replyLang: String = "", // empty = follow user language
    val ragEnabled: Boolean = true,
    val gen: GenParams = GenParams(),
    val preset: String = "balanced", // precise | balanced | creative | custom
    val onboardingDone: Boolean = false // v1.4 — first-run wizard shown once
)

class SettingsRepository(private val context: Context) {

    private object K {
        val themeMode = intPreferencesKey("theme_mode")
        val dynamicColors = booleanPreferencesKey("dynamic_colors")
        val language = stringPreferencesKey("language")
        val threads = intPreferencesKey("threads")
        val contextSize = intPreferencesKey("context_size")
        val batchSize = intPreferencesKey("batch_size")
        val gpuLayers = intPreferencesKey("gpu_layers")
        val flashAttention = booleanPreferencesKey("flash_attention")
        val maxRamPct = intPreferencesKey("max_ram_pct")
        val autoUnload = booleanPreferencesKey("auto_unload")
        val allowNetwork = booleanPreferencesKey("allow_network")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val replyLang = stringPreferencesKey("reply_lang")
        val ragEnabled = booleanPreferencesKey("rag_enabled")
        val temperature = floatPreferencesKey("gen_temperature")
        val topK = intPreferencesKey("gen_top_k")
        val topP = floatPreferencesKey("gen_top_p")
        val minP = floatPreferencesKey("gen_min_p")
        val repeatPenalty = floatPreferencesKey("gen_repeat_penalty")
        val maxTokens = intPreferencesKey("gen_max_tokens")
        val seed = stringPreferencesKey("gen_seed")
        val preset = stringPreferencesKey("gen_preset")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[K.themeMode] ?: 0,
            dynamicColors = p[K.dynamicColors] ?: true,
            language = p[K.language] ?: "system",
            threads = p[K.threads] ?: 0,
            contextSize = p[K.contextSize] ?: 2048,
            batchSize = p[K.batchSize] ?: 256,
            gpuLayers = p[K.gpuLayers] ?: 0,
            flashAttention = p[K.flashAttention] ?: false,
            maxRamPct = p[K.maxRamPct] ?: 60,
            autoUnload = p[K.autoUnload] ?: true,
            allowNetwork = p[K.allowNetwork] ?: false,
            pinHash = p[K.pinHash],
            pinSalt = p[K.pinSalt],
            systemPrompt = p[K.systemPrompt] ?: "",
            replyLang = p[K.replyLang] ?: "",
            ragEnabled = p[K.ragEnabled] ?: true,
            gen = GenParams(
                temperature = p[K.temperature] ?: 0.7f,
                topK = p[K.topK] ?: 40,
                topP = p[K.topP] ?: 0.95f,
                minP = p[K.minP] ?: 0.05f,
                repeatPenalty = p[K.repeatPenalty] ?: 1.1f,
                maxTokens = p[K.maxTokens] ?: 512,
                seed = p[K.seed]?.toLongOrNull() ?: -1L
            ),
            preset = p[K.preset] ?: "balanced",
            onboardingDone = p[K.onboardingDone] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(v: Int) = set(K.themeMode, v)
    suspend fun setDynamicColors(v: Boolean) = set(K.dynamicColors, v)
    suspend fun setLanguage(v: String) = set(K.language, v)
    suspend fun setThreads(v: Int) = set(K.threads, v)
    suspend fun setContextSize(v: Int) = set(K.contextSize, v)
    suspend fun setBatchSize(v: Int) = set(K.batchSize, v)
    suspend fun setGpuLayers(v: Int) = set(K.gpuLayers, v)
    suspend fun setFlashAttention(v: Boolean) = set(K.flashAttention, v)
    suspend fun setMaxRamPct(v: Int) = set(K.maxRamPct, v)
    suspend fun setAutoUnload(v: Boolean) = set(K.autoUnload, v)
    suspend fun setAllowNetwork(v: Boolean) = set(K.allowNetwork, v)
    suspend fun setPin(hash: String?, salt: String?) {
        context.dataStore.edit { p ->
            if (hash == null || salt == null) {
                p.remove(K.pinHash); p.remove(K.pinSalt)
            } else {
                p[K.pinHash] = hash; p[K.pinSalt] = salt
            }
        }
    }
    suspend fun setSystemPrompt(v: String) = set(K.systemPrompt, v)
    suspend fun setReplyLang(v: String) = set(K.replyLang, v)
    suspend fun setRagEnabled(v: Boolean) = set(K.ragEnabled, v)
    suspend fun setPreset(v: String) = set(K.preset, v)
    suspend fun setOnboardingDone(v: Boolean) = set(K.onboardingDone, v)
    suspend fun setGenParams(g: GenParams) {
        context.dataStore.edit { p ->
            p[K.temperature] = g.temperature
            p[K.topK] = g.topK
            p[K.topP] = g.topP
            p[K.minP] = g.minP
            p[K.repeatPenalty] = g.repeatPenalty
            p[K.maxTokens] = g.maxTokens
            p[K.seed] = g.seed.toString()
        }
    }

    private suspend fun <T> set(key: androidx.datastore.preferences.core.Preferences.Key<T>, v: T) {
        context.dataStore.edit { it[key] = v }
    }
}
