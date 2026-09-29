package com.drs.ai.core.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.drs.ai.core.ai.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * On-device vision: chat model + mmproj projector via llama.cpp mtmd.
 * Honest gating: reports exactly which of the two required model files is missing.
 */
class VisionEngine(private val context: android.content.Context, private val chatEngine: LlmEngine) {

    private val mutex = Mutex()
    private var handle: Long = 0L
    private var loadedChatPath: String? = null
    private var loadedMmprojPath: String? = null
    var lastError: String? = null
        private set

    val isLoaded: Boolean get() = handle != 0L

    /** Returns null on success or a precise error naming the missing/failed piece. */
    suspend fun ensureLoaded(chatPath: String, mmprojPath: String, nCtx: Int, nThreads: Int): String? = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!VisionNative.available) return@withContext "Native vision library unavailable on this device"
            if (handle != 0L && loadedChatPath == chatPath && loadedMmprojPath == mmprojPath) return@withContext null
            unloadLocked()
            val h = try { VisionNative.nativeVisionCreate() } catch (t: Throwable) {
                return@withContext "Vision init failed: ${t.message}"
            }
            if (h == 0L) return@withContext "Vision init failed (null handle)"
            val err = VisionNative.nativeVisionLoad(h, chatPath, mmprojPath, nCtx, nThreads)
            if (err != null) {
                VisionNative.nativeVisionDestroy(h)
                lastError = err
                return@withContext err
            }
            handle = h
            loadedChatPath = chatPath
            loadedMmprojPath = mmprojPath
            lastError = null
            null
        }
    }

    suspend fun unload() = mutex.withLock { withContext(Dispatchers.IO) { unloadLocked() } }

    private fun unloadLocked() {
        if (handle != 0L) {
            try { VisionNative.nativeVisionDestroy(handle) } catch (_: Throwable) {}
            handle = 0L
            loadedChatPath = null
            loadedMmprojPath = null
        }
    }

    /** Decode, downscale, JPEG-encode and analyze. Streams via onToken like chat. */
    suspend fun analyze(imageUri: Uri, question: String, maxTokens: Int = 512, temperature: Float = 0.3f, onToken: (String) -> Unit): String = withContext(Dispatchers.IO) {
        if (handle == 0L) throw IllegalStateException("Vision models not loaded")
        val jpeg = jpegBytes(imageUri)
        val sink = object : VisionNative.Sink {
            override fun onToken(piece: String): Boolean = try { onToken(piece); true } catch (t: Throwable) { false }
        }
        val err = VisionNative.nativeVisionGenerate(handle, question, jpeg, maxTokens, temperature, sink)
        if (err != null) throw IllegalStateException(err)
        ""
    }

    private fun jpegBytes(uri: Uri, maxSide: Int = 512, quality: Int = 90): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalStateException("Cannot decode image")
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide && bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Cannot decode image")
        val scaled = if (bmp.width > maxSide || bmp.height > maxSide) {
            val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true)
        } else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== bmp) scaled.recycle()
        bmp.recycle()
        return out.toByteArray()
    }
}
