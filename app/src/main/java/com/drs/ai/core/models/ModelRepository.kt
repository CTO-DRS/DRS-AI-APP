package com.drs.ai.core.models

import android.content.Context
import android.net.Uri
import com.drs.ai.data.dao.MemoryAndModelsDao
import com.drs.ai.data.db.LocalModel
import com.drs.ai.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class CheckItem(val name: String, val status: Status, val note: String) {
    enum class Status { PASS, WARN, FAIL }
}

data class CompatReport(val overall: CheckItem.Status, val items: List<CheckItem>)

/**
 * Model lifecycle: import via SAF, one-time HTTPS download (privacy-gated), deletion,
 * kind-scoped activation and hardware compatibility checks.
 */
class ModelRepository(
    private val context: Context,
    private val dao: MemoryAndModelsDao,
    private val settings: SettingsRepository
) {
    val modelsDir: File = File(context.filesDir, "models").apply { mkdirs() }
    val tmpDir: File = File(context.filesDir, "tmp").apply { mkdirs() }

    fun observeModels(): Flow<List<LocalModel>> = dao.observeModels()

    suspend fun activeModel(kind: String): LocalModel? = dao.getActiveModel(kind)

    fun observeActiveChat(): Flow<LocalModel?> = dao.observeActiveModel(KIND_CHAT)

    private fun freeBytes(): Long = modelsDir.usableSpace

    private fun formatBytes(b: Long): String = when {
        b >= 1 shl 30 -> "%.2f GB".format(b / 1073741824f)
        b >= 1 shl 20 -> "%.1f MB".format(b / 1048576f)
        b >= 1 shl 10 -> "%.0f KB".format(b / 1024f)
        else -> "$b B"
    }

    /**
     * Import from a SAF Uri. Copies to private storage, validates magic, parses header.
     * Returns the new model id or throws [ImportException] with a readable message.
     */
    suspend fun import(uri: Uri, kind: String, onProgress: (Int) -> Unit = {}): Long = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (size <= 0) throw ImportException("Cannot read selected file")
        if (size > freeBytes()) throw ImportException("Not enough storage: need ${formatBytes(size)}")

        val dest = File(modelsDir, "m_${System.currentTimeMillis()}.bin")
        try {
            resolver.openInputStream(uri)?.use { ins ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(1 shl 20)
                    var copied = 0L
                    var lastPct = -1
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        copied += n
                        val pct = (copied * 100 / size).toInt()
                        if (pct != lastPct) { lastPct = pct; onProgress(pct) }
                    }
                }
            } ?: throw ImportException("Cannot open selected file")

            // validate
            var info: GgufParser.GgufInfo? = null
            when (kind) {
                KIND_WHISPER -> if (!GgufParser.isWhisperGgml(dest)) {
                    throw ImportException("Not a whisper GGML model file (bad magic)")
                }
                else -> {
                    if (!GgufParser.isGguf(dest)) throw ImportException("Not a GGUF file (bad magic)")
                    // v1.4.1 — deep validation (tensor table + data coverage) so truncated
                    // downloads are rejected at import time with a precise message
                    info = try { GgufParser.validateFull(dest) } catch (e: Exception) {
                        throw ImportException("GGUF validation failed: ${e.message}")
                    }
                    if (kind == KIND_MMPROJ && !info.isMmproj) {
                        throw ImportException("This GGUF is not a vision projector (arch=${info.arch})")
                    }
                    if (kind == KIND_CHAT && info.isMmproj) {
                        throw ImportException("This is a vision projector (mmproj), import it with kind 'Vision (mmproj)'")
                    }
                }
            }

            val display = resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: "model_${System.currentTimeMillis()}"

            val entity = LocalModel(
                name = display,
                path = dest.absolutePath,
                sizeBytes = dest.length(),
                kind = kind,
                arch = info?.arch,
                ctxLen = info?.ctxLength,
                quant = info?.quantLabel,
                active = false,
                addedAt = System.currentTimeMillis()
            )
            dao.insertModel(entity)
        } catch (e: ImportException) {
            dest.delete()
            throw e
        } catch (e: Exception) {
            dest.delete()
            throw ImportException(e.message ?: "import failed")
        }
    }

    /** One-time HTTPS download, only allowed when Privacy Center permits network. */
    suspend fun download(url: String, fileName: String, kind: String, onProgress: (Int) -> Unit): Long = withContext(Dispatchers.IO) {
        if (!settings.current().allowNetwork) throw ImportException("network_blocked")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) throw ImportException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            val dest = File(modelsDir, "m_${System.currentTimeMillis()}.bin")
            conn.inputStream.use { ins ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(1 shl 20)
                    var copied = 0L
                    var lastPct = -1
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        copied += n
                        if (total > 0) {
                            val pct = (copied * 100 / total).toInt()
                            if (pct != lastPct) { lastPct = pct; onProgress(pct) }
                        }
                    }
                }
            }
            if (kind == KIND_WHISPER && !GgufParser.isWhisperGgml(dest)) {
                dest.delete(); throw ImportException("Downloaded file is not a whisper GGML model")
            }
            if (kind != KIND_WHISPER && !GgufParser.isGguf(dest)) {
                dest.delete(); throw ImportException("Downloaded file is not a GGUF model")
            }
            // v1.4.1 — deep validation: a truncated download must be rejected, not imported
            val info = if (kind != KIND_WHISPER) try {
                GgufParser.validateFull(dest)
            } catch (e: Exception) {
                dest.delete(); throw ImportException("GGUF validation failed: ${e.message}")
            } else null
            val id = dao.insertModel(
                LocalModel(
                    name = fileName, path = dest.absolutePath, sizeBytes = dest.length(), kind = kind,
                    arch = info?.arch, ctxLen = info?.ctxLength, quant = info?.quantLabel,
                    active = false, addedAt = System.currentTimeMillis()
                )
            )
            id
        } finally {
            conn.disconnect()
        }
    }

    suspend fun delete(model: LocalModel) = withContext(Dispatchers.IO) {
        File(model.path).delete()
        dao.deleteModel(model.id)
    }

    suspend fun setActive(model: LocalModel) = withContext(Dispatchers.IO) {
        dao.clearActiveForKind(model.kind)
        dao.updateModel(model.copy(active = true))
    }

    fun compatReport(model: LocalModel, profile: HardwareProfile): CompatReport {
        val items = mutableListOf<CheckItem>()
        items += CheckItem(
            "CPU architecture", if (profile.abi == "arm64-v8a") CheckItem.Status.PASS else CheckItem.Status.FAIL,
            "Device ABI: ${profile.abi} — this build ships arm64-v8a native libraries"
        )
        val androidOk = android.os.Build.VERSION.SDK_INT >= 26
        items += CheckItem(
            "Android version", if (androidOk) CheckItem.Status.PASS else CheckItem.Status.FAIL,
            "Android ${profile.androidVersion} (min 8.0)"
        )
        val needed = (model.sizeBytes * 1.2).toLong()
        items += if (profile.storageFreeGB * 1_000_000_000f > needed) CheckItem(
            "Storage", CheckItem.Status.PASS, "Free ${"%.1f".format(profile.storageFreeGB)} GB for ${formatBytes(model.sizeBytes)} model"
        ) else CheckItem("Storage", CheckItem.Status.FAIL, "Need ~${formatBytes(needed)}, only ${"%.1f".format(profile.storageFreeGB)} GB free")

        val budgetBytes = profile.ramTotalGB * 1_000_000_000f * 0.6f
        val working = model.sizeBytes * 1.35 // weights + KV + activations
        items += when {
            working <= budgetBytes -> CheckItem("RAM budget", CheckItem.Status.PASS,
                "~${formatBytes(working.toLong())} working set fits ${"%.0f".format(60)}% RAM budget")
            working <= budgetBytes * 1.5 -> CheckItem("RAM budget", CheckItem.Status.WARN,
                "Tight: ~${formatBytes(working.toLong())} vs budget ${formatBytes(budgetBytes.toLong())} — close other apps, lower context")
            else -> CheckItem("RAM budget", CheckItem.Status.FAIL,
                "~${formatBytes(working.toLong())} exceeds what ${"%.1f".format(profile.ramTotalGB)}GB can host — use a smaller model (Q4_K_M, 0.5–1.5B)")
        }

        val ctx = model.ctxLen
        if (ctx != null) {
            items += if (ctx <= profile.maxContext) CheckItem(
                "Context length", CheckItem.Status.PASS, "Model $ctx ≤ device max ${profile.maxContext}"
            ) else CheckItem(
                "Context length", CheckItem.Status.WARN,
                "Model supports $ctx; on this device run with a smaller runtime context (≤ ${profile.maxContext})"
            )
        }

        if (model.kind == KIND_CHAT) {
            items += if ((profile.ramTotalGB >= 6f)) CheckItem(
                "Chat readiness", CheckItem.Status.PASS, "Balanced-or-better device profile"
            ) else CheckItem(
                "Chat readiness", CheckItem.Status.WARN,
                "Entry device: prefer 0.5–1.5B Q4 models, context ≤ 2048, 2 threads"
            )
        }
        if (model.quant != null && (model.quant.startsWith("F16") || model.quant.startsWith("F32"))) {
            items += CheckItem("Quantization", CheckItem.Status.WARN,
                "Unquantized ${model.quant} is large for mobile — Q4_K_M / Q5_K_M recommended")
        } else {
            items += CheckItem("Quantization", CheckItem.Status.PASS, "${model.quant ?: "standard"} is mobile-friendly")
        }

        val overall = if (items.any { it.status == CheckItem.Status.FAIL }) CheckItem.Status.FAIL
        else if (items.any { it.status == CheckItem.Status.WARN }) CheckItem.Status.WARN
        else CheckItem.Status.PASS
        return CompatReport(overall, items)
    }

    class ImportException(message: String) : Exception(message)

    companion object {
        const val KIND_CHAT = "chat"
        const val KIND_EMBEDDER = "embedder"
        const val KIND_MMPROJ = "mmproj"
        const val KIND_WHISPER = "whisper"
        val KINDS = listOf(KIND_CHAT, KIND_EMBEDDER, KIND_MMPROJ, KIND_WHISPER)
    }
}
