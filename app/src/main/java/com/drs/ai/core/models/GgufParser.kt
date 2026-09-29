package com.drs.ai.core.models

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-Kotlin GGUF (v2/v3) header parser. Reads only the header + metadata KV pairs,
 * never the tensor payload — safe for multi-GB files.
 */
object GgufParser {

    const val GGUF_MAGIC = "GGUF"
    const val WHISPER_MAGIC = "ggml"

    data class GgufInfo(
        val version: Int,
        val arch: String,
        val name: String,
        val ctxLength: Long?,
        val blockCount: Long?,
        val embeddingLength: Long?,
        val quantLabel: String,
        val isMmproj: Boolean
    )

    class GgufException(message: String) : Exception(message)

    /** Returns true if the file starts with the GGUF magic. */
    fun isGguf(file: File): Boolean = file.inputStream().use { ins ->
        val m = ByteArray(4)
        ins.read(m) == 4 && String(m, Charsets.US_ASCII) == GGUF_MAGIC
    }

    /** Returns true if the file looks like a whisper.cpp GGML model. */
    fun isWhisperGgml(file: File): Boolean = file.inputStream().use { ins ->
        val m = ByteArray(4)
        ins.read(m) == 4 && String(m, Charsets.US_ASCII) == WHISPER_MAGIC
    }

    fun parse(file: File): GgufInfo = parseImpl(file, deep = false)

    /**
     * v1.4.1 — deep validation: header + KV + the FULL tensor table + a data-coverage
     * check against the actual file size. Catches incomplete downloads (the most common
     * cause of native load failures) with a precise "missing N MB" message, so users
     * get an honest, actionable error instead of a generic native one.
     */
    fun validateFull(file: File): GgufInfo = parseImpl(file, deep = true)

    private fun parseImpl(file: File, deep: Boolean): GgufInfo {
        RandomAccessFile(file, "r").use { raf ->
            val magic = ByteArray(4)
            raf.readFully(magic)
            if (String(magic, Charsets.US_ASCII) != GGUF_MAGIC) throw GgufException("Not a GGUF file")
            val version = readU32(raf).toInt()
            if (version < 2 || version > 3) throw GgufException("Unsupported GGUF version: $version")
            val tensorCount = readU64(raf)
            val kvCount = readU64(raf)
            if (kvCount > 100_000) throw GgufException("Corrupt GGUF header (kv count $kvCount)")

            var arch = ""
            var name = ""
            var ctx: Long? = null
            var blocks: Long? = null
            var emb: Long? = null
            var fileType: Long? = null
            var alignment = 32L

            repeat(kvCount.toInt()) {
                val key = readString(raf)
                val type = readU32(raf).toInt()
                when {
                    key == "general.architecture" && type == 8 -> arch = readString(raf)
                    key == "general.name" && type == 8 -> name = readString(raf)
                    key == "general.alignment" && type != 8 && type != 9 -> alignment = readScalarLong(type, raf) ?: 32L
                    key == "general.file_type" && type != 8 && type != 9 -> fileType = readScalarLong(type, raf)
                    key.endsWith(".context_length") && type != 8 && type != 9 -> ctx = readScalarLong(type, raf)
                    key.endsWith(".block_count") && type != 8 && type != 9 -> blocks = readScalarLong(type, raf)
                    key.endsWith(".embedding_length") && type != 8 && type != 9 -> emb = readScalarLong(type, raf)
                    type == 8 -> readString(raf)
                    type == 9 -> skipArray(raf)
                    else -> skipScalar(type, raf)
                }
            }
            val quant = quantLabel(fileType)
            val info = GgufInfo(version, arch.ifBlank { "unknown" }, name, ctx, blocks, emb, quant, arch == "clip")
            if (!deep) return info

            // ---- v1.4.1 deep validation: walk the full tensor table ----------------
            if (tensorCount > 100_000) throw GgufException("Corrupt GGUF header (tensor count $tensorCount)")
            val align = if (alignment in 1..(1 shl 20)) alignment else 32L
            val headerEnd = raf.filePointer
            val dataStart = ((headerEnd + align - 1) / align) * align

            var maxOffset = -1L
            var maxType = -1
            var maxNElems = 0L
            repeat(tensorCount.toInt()) {
                val tName = readString(raf)
                val nDims = readU32(raf).toInt()
                if (nDims < 1 || nDims > 4) throw GgufException("Corrupt GGUF tensor entry ($tName: n_dims=$nDims)")
                var nElems = 1L
                repeat(nDims) {
                    val d = readU64(raf)
                    if (d == 0L || d > (1L shl 34)) throw GgufException("Corrupt GGUF tensor entry ($tName: dim=$d)")
                    nElems = if (nElems > (1L shl 50) / d.coerceAtLeast(1)) (1L shl 50) else nElems * d // overflow-safe cap
                }
                val tType = readU32(raf).toInt()
                val off = readU64(raf)
                if (off > maxOffset) { maxOffset = off; maxType = tType; maxNElems = nElems }
            }

            // The last tensor (highest offset) ends at dataStart + offset + size.
            // A size-exact check for well-known types, a >=1-byte bound otherwise.
            val lastSize = tensorBytes(maxType, maxNElems) ?: 1L
            val fileLen = file.length()
            val dataEnd = dataStart + maxOffset + lastSize
            if (dataEnd > fileLen) {
                val needMb = (dataEnd - dataStart) / (1024 * 1024)
                val missingMb = (dataEnd - fileLen + 1024 * 1024 - 1) / (1024 * 1024)
                throw GgufException(
                    "GGUF file is truncated: tensor data needs ~${needMb} MB but the file ends ~${missingMb} MB short — re-download and re-import"
                )
            }
            return info
        }
    }

    /** Exact byte size for well-known ggml tensor types (null = cannot compute). */
    private fun tensorBytes(type: Int, nElems: Long): Long? = when (type) {
        0 -> nElems * 4                                     // F32
        1 -> nElems * 2                                     // F16
        2 -> blocks(nElems, 32) * 18                        // Q4_0
        3 -> blocks(nElems, 32) * 20                        // Q4_1
        6 -> blocks(nElems, 32) * 22                        // Q5_0
        7 -> blocks(nElems, 32) * 24                        // Q5_1
        8 -> blocks(nElems, 32) * 34                        // Q8_0
        10, 11, 12, 13 -> blocks(nElems, 256) * 84          // Q2_K / Q3_K*
        14, 15 -> blocks(nElems, 256) * 144                 // Q4_K*
        16, 17 -> blocks(nElems, 256) * 176                 // Q5_K*
        18 -> blocks(nElems, 256) * 210                     // Q6_K
        29 -> nElems                                        // I8
        30 -> nElems * 2                                    // I16
        31 -> nElems * 4                                    // I32
        32 -> nElems * 8                                    // I64
        33 -> nElems * 8                                    // F64
        else -> null                                        // IQ*/BF16/unknown → weak bound only
    }

    private fun blocks(nElems: Long, per: Long): Long = (nElems + per - 1) / per

    /** Human label for general.file_type per GGUF spec. */
    fun quantLabel(fileType: Long?): String = when (fileType?.toInt()) {
        null -> "unknown"
        0 -> "F32"; 1 -> "F16"
        2 -> "Q4_0"; 3 -> "Q4_1"
        7 -> "Q8_0"
        8 -> "Q5_0"; 9 -> "Q5_1"
        10 -> "Q2_K"; 11 -> "Q3_K_S"; 12 -> "Q3_K_M"; 13 -> "Q3_K_L"
        14 -> "Q4_K_S"; 15 -> "Q4_K_M"
        16 -> "Q5_K_S"; 17 -> "Q5_K_M"
        18 -> "Q6_K"
        19 -> "IQ2_XXS"; 20 -> "IQ2_XS"; 21 -> "Q2_K_S"
        22 -> "IQ3_XS"; 23 -> "IQ3_XXS"; 24 -> "IQ1_S"
        25 -> "IQ4_NL"; 26 -> "IQ3_S"; 27 -> "IQ3_M"
        28 -> "IQ2_S"; 29 -> "IQ2_M"; 30 -> "IQ4_XS"
        31 -> "IQ1_M"; 32 -> "BF16"
        36 -> "TQ1_0"; 37 -> "TQ2_0"
        else -> "type $fileType"
    }

    // ---- low-level readers -------------------------------------------------

    private fun readScalarLong(type: Int, raf: RandomAccessFile): Long? = when (type) {
        0, 1 -> { val b = ByteArray(1); raf.readFully(b); b[0].toLong() }
        2 -> { val b = ByteArray(2); raf.readFully(b); ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toLong() and 0xFFFF }
        3 -> { val b = ByteArray(2); raf.readFully(b); ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toLong() }
        4 -> readU32(raf)
        5 -> { val b = ByteArray(4); raf.readFully(b); ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int.toLong() }
        6 -> { val b = ByteArray(4); raf.readFully(b); ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).float.toLong() }
        7 -> { val b = ByteArray(1); raf.readFully(b); if (b[0].toInt() != 0) 1L else 0L }
        10, 11, 12 -> readU64(raf)
        else -> null
    }

    private fun readU32(raf: RandomAccessFile): Long {
        val b = ByteArray(4); raf.readFully(b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
    }

    private fun readU64(raf: RandomAccessFile): Long {
        val b = ByteArray(8); raf.readFully(b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readString(raf: RandomAccessFile): String {
        val len = readU64(raf)
        if (len > 64 * 1024 * 1024) throw GgufException("GGUF string too long: $len")
        val b = ByteArray(len.toInt())
        raf.readFully(b)
        return String(b, Charsets.UTF_8)
    }

    private fun skipScalar(type: Int, raf: RandomAccessFile) {
        val size = when (type) {
            0, 1, 7 -> 1
            2, 3 -> 2
            4, 5, 6 -> 4
            10, 11, 12 -> 8
            else -> throw GgufException("Unknown GGUF scalar type $type")
        }
        raf.seek(raf.filePointer + size)
    }

    private fun skipArray(raf: RandomAccessFile) {
        val elemType = readU32(raf).toInt()
        val count = readU64(raf)
        when (elemType) {
            8 -> repeat(count.toInt().coerceAtMost(10_000_000)) { readString(raf) }
            9 -> throw GgufException("Nested GGUF arrays unsupported")
            else -> {
                val size = when (elemType) {
                    0, 1, 7 -> 1; 2, 3 -> 2; 4, 5, 6 -> 4; 10, 11, 12 -> 8
                    else -> throw GgufException("Unknown GGUF array element type $elemType")
                }
                raf.seek(raf.filePointer + size * count)
            }
        }
    }
}
