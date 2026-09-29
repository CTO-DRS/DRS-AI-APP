package com.drs.ai.core.models

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import android.content.pm.PackageManager

enum class DeviceProfile { ENTRY, BALANCED, PERFORMANCE, EXTREME }

data class HardwareProfile(
    val ramTotalGB: Float,
    val ramAvailGB: Float,
    val cores: Int,
    val abi: String,
    val androidVersion: String,
    val vulkanVersion: String?, // null = not available
    val lowRamDevice: Boolean,
    val storageFreeGB: Float,
    val level: DeviceProfile,
    val maxContext: Int,
    val maxModelBytes: Long,
    val recThreads: Int,
    val recBatch: Int
) {
    fun profileName(): String = when (level) {
        DeviceProfile.ENTRY -> "entry"
        DeviceProfile.BALANCED -> "balanced"
        DeviceProfile.PERFORMANCE -> "performance"
        DeviceProfile.EXTREME -> "extreme"
    }
}

object HardwareProfiler {

    fun probe(context: Context): HardwareProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val ramTotalGB = mem.totalMem / 1024f / 1024f / 1024f
        val ramAvailGB = mem.availMem / 1024f / 1024f / 1024f
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val lowRam = am.isLowRamDevice

        val vulkan = detectVulkan(context)

        val stat = StatFs(context.filesDir.absolutePath)
        val storageFreeGB = stat.availableBytes / 1024f / 1024f / 1024f

        val level = when {
            lowRam || ramTotalGB < 4f -> DeviceProfile.ENTRY
            ramTotalGB < 8f -> DeviceProfile.BALANCED
            ramTotalGB < 16f -> DeviceProfile.PERFORMANCE
            else -> DeviceProfile.EXTREME
        }

        val (maxCtx, maxModel, threads, batch) = when (level) {
            DeviceProfile.ENTRY -> Quad(2048, 1_400_000_000L, 2.coerceAtMost(cores), 128)
            DeviceProfile.BALANCED -> Quad(4096, 2_600_000_000L, (cores - 1).coerceIn(2, 4), 256)
            DeviceProfile.PERFORMANCE -> Quad(8192, 5_200_000_000L, (cores - 2).coerceIn(4, 6), 256)
            DeviceProfile.EXTREME -> Quad(16384, 8_000_000_000L, (cores - 2).coerceIn(6, 8), 512)
        }

        return HardwareProfile(
            ramTotalGB = ramTotalGB,
            ramAvailGB = ramAvailGB,
            cores = cores,
            abi = abi,
            androidVersion = Build.VERSION.RELEASE,
            vulkanVersion = vulkan,
            lowRamDevice = lowRam,
            storageFreeGB = storageFreeGB,
            level = level,
            maxContext = maxCtx,
            maxModelBytes = maxModel,
            recThreads = threads,
            recBatch = batch
        )
    }

    private fun detectVulkan(context: Context): String? {
        val pm = context.packageManager
        return try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
                // Encoded feature version major.minor.patch -> readable
                val has11 = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x00401000)
                if (has11) "1.1+" else "1.0+"
            } else null
        } catch (_: Throwable) { null }
    }

    private data class Quad(val a: Int, val b: Long, val c: Int, val d: Int)
}
