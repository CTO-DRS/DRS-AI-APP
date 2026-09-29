package com.drs.ai.core.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Local voice: AudioRecord 16 kHz capture → whisper GGML transcription → system TTS
 * for speaking. STT never touches the network.
 */
class VoiceEngine(private val context: Context) {

    private val mutex = Mutex()
    private var handle: Long = 0L
    private var loadedPath: String? = null
    var lastError: String? = null
        private set

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    val isLoaded: Boolean get() = handle != 0L

    @Volatile var recordingCancelled: Boolean = false

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    suspend fun ensureLoaded(modelPath: String, threads: Int): String? = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!WhisperNative.available) return@withContext "Native whisper library unavailable on this device"
            if (handle != 0L && loadedPath == modelPath) return@withContext null
            unloadLocked()
            val h = try { WhisperNative.nativeWhisperCreate() } catch (t: Throwable) {
                return@withContext "Whisper init failed: ${t.message}"
            }
            if (h == 0L) return@withContext "Whisper init failed (null handle)"
            val err = WhisperNative.nativeWhisperLoad(h, modelPath, threads)
            if (err != null) {
                WhisperNative.nativeWhisperDestroy(h)
                lastError = err
                return@withContext err
            }
            handle = h
            loadedPath = modelPath
            lastError = null
            null
        }
    }

    suspend fun unload() = mutex.withLock { withContext(Dispatchers.IO) { unloadLocked() } }

    private fun unloadLocked() {
        if (handle != 0L) {
            try { WhisperNative.nativeWhisperDestroy(handle) } catch (_: Throwable) {}
            handle = 0L
            loadedPath = null
        }
    }

    suspend fun transcribe(pcm: FloatArray, language: String? = null, translate: Boolean = false): String = withContext(Dispatchers.IO) {
        if (handle == 0L) throw IllegalStateException("STT model not loaded")
        val text = WhisperNative.nativeWhisperTranscribe(handle, pcm, language, translate)
        if (text == null) {
            val err = WhisperNative.nativeWhisperLastError(handle)
            throw IllegalStateException(err ?: "Transcription failed")
        }
        text.trim()
    }

    /** Blocking capture of up to [maxSeconds] seconds. Returns PCM floats (16 kHz mono). */
    fun record(maxSeconds: Int = 60, onLevel: (Float) -> Unit = {}): FloatArray {
        if (!hasMicPermission()) throw IllegalStateException("mic_permission")
        recordingCancelled = false
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) throw IllegalStateException("AudioRecord unavailable")
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        } catch (e: Exception) {
            throw IllegalStateException("AudioRecord init failed: ${e.message}")
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("AudioRecord not initialized")
        }
        val out = ArrayList<Float>((maxSeconds * sampleRate).coerceAtMost(30 * sampleRate))
        val shortBuf = ShortArray(1024)
        record.startRecording()
        val started = System.currentTimeMillis()
        try {
            while ((System.currentTimeMillis() - started) / 1000f < maxSeconds && !recordingCancelled) {
                val n = record.read(shortBuf, 0, shortBuf.size)
                if (n <= 0) break
                var peak = 0f
                for (i in 0 until n) {
                    val v = shortBuf[i] / 32768f
                    out.add(v)
                    val a = Math.abs(v)
                    if (a > peak) peak = a
                }
                onLevel(peak)
            }
        } finally {
            record.stop()
            record.release()
        }
        return out.toFloatArray()
    }

    // ---- TTS ---------------------------------------------------------------

    fun initTts(onReady: (Boolean) -> Unit) {
        if (tts != null) { onReady(ttsReady); return }
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                val offline = hasOfflineVoice()
                ttsReady = offline
            }
            onReady(ttsReady)
        }
    }

    private fun hasOfflineVoice(): Boolean {
        val t = tts ?: return false
        return try {
            val voices = t.voices ?: return true // assume system handles it
            voices.any { !it.isNetworkConnectionRequired }
        } catch (_: Throwable) { true }
    }

    fun speak(text: String): Boolean {
        val t = tts ?: return false
        if (!ttsReady) return false
        return try {
            val res = t.setLanguage(Locale.getDefault())
            val ok = res == TextToSpeech.LANG_AVAILABLE || res == TextToSpeech.LANG_COUNTRY_AVAILABLE || res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            if (!ok) t.setLanguage(Locale.US)
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "drs_${System.currentTimeMillis()}")
            true
        } catch (_: Throwable) { false }
    }

    fun shutdownTts() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Throwable) {}
        tts = null
        ttsReady = false
    }

    /** Save raw PCM floats as a 16-bit WAV file (for reference/inspection). */
    fun saveWav(pcm: FloatArray, dest: File) {
        val sampleRate = 16000
        val data = ByteArray(pcm.size * 2)
        for (i in pcm.indices) {
            val s = (pcm[i].coerceIn(-1f, 1f) * 32767).toInt()
            data[i * 2] = (s and 0xFF).toByte()
            data[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        val header = ByteArray(44)
        fun le32(off: Int, v: Int) { header[off] = (v and 0xFF).toByte(); header[off+1] = ((v shr 8) and 0xFF).toByte(); header[off+2] = ((v shr 16) and 0xFF).toByte(); header[off+3] = ((v shr 24) and 0xFF).toByte() }
        fun le16(off: Int, v: Int) { header[off] = (v and 0xFF).toByte(); header[off+1] = ((v shr 8) and 0xFF).toByte() }
        "RIFF".toByteArray().copyInto(header, 0)
        le32(4, 36 + data.size)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        le32(16, 16); le16(20, 1); le16(22, 1); le32(24, sampleRate); le32(28, sampleRate * 2); le16(32, 2); le16(34, 16)
        "data".toByteArray().copyInto(header, 36)
        le32(40, data.size)
        dest.outputStream().use { it.write(header); it.write(data) }
    }
}
