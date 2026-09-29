package com.drs.ai.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import com.drs.ai.core.ai.LlmEngine
import com.drs.ai.core.models.HardwareProfile
import com.drs.ai.core.models.HardwareProfiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real OS-level metrics only. Nothing invented, nothing sampled from thin air.
 */
class DiagnosticsCollector(
    private val context: Context,
    private val chatEngine: LlmEngine,
    private val embedderEngine: LlmEngine
) {
    data class Report(
        val device: String,
        val androidVersion: String,
        val abi: String,
        val cores: Int,
        val ramTotalGB: Float,
        val ramAvailGB: Float,
        val lowRamDevice: Boolean,
        val storageFreeGB: Float,
        val thermal: String,
        val vulkan: String,
        val profile: String,
        val maxContext: Int,
        val chatEngineLoaded: Boolean,
        val chatModel: String?,
        val chatCtxUsed: Int,
        val embedderLoaded: Boolean,
        val threads: Int
    )

    suspend fun collect(threads: Int): Report = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val stat = StatFs(context.filesDir.absolutePath)
        val profile = HardwareProfiler.probe(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = thermalLabel(pm)

        Report(
            device = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            cores = Runtime.getRuntime().availableProcessors(),
            ramTotalGB = mem.totalMem / 1073741824f,
            ramAvailGB = mem.availMem / 1073741824f,
            lowRamDevice = am.isLowRamDevice,
            storageFreeGB = stat.availableBytes / 1073741824f,
            thermal = thermal,
            vulkan = vulkanLabel(profile),
            profile = profile.profileName(),
            maxContext = profile.maxContext,
            chatEngineLoaded = chatEngine.isLoaded,
            chatModel = chatEngine.loadedModelName,
            chatCtxUsed = chatEngine.ctxUsed(),
            embedderLoaded = embedderEngine.isLoaded,
            threads = threads
        )
    }

    private fun thermalLabel(pm: PowerManager): String {
        if (Build.VERSION.SDK_INT < 29) return "unavailable (API < 29)"
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> "normal"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderate throttling"
            PowerManager.THERMAL_STATUS_SEVERE, PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY, PowerManager.THERMAL_STATUS_SHUTDOWN -> "severe throttling"
            else -> "unknown"
        }
    }

    private fun vulkanLabel(profile: HardwareProfile): String {
        val has = try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        } catch (_: Throwable) { false }
        return if (has) "available (${profile.vulkanVersion ?: "yes"}) — CPU used for inference in this build"
        else "not available — CPU inference"
    }
}
