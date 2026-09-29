package com.drs.ai.core.rag

import kotlin.math.sqrt

/** Little-endian float codec + cosine similarity over local vectors. */
object VectorMath {

    fun pack(v: FloatArray): ByteArray {
        val bb = java.nio.ByteBuffer.allocate(v.size * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (f in v) bb.putFloat(f)
        return bb.array()
    }

    fun unpack(b: ByteArray): FloatArray {
        val bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(b.size / 4)
        for (i in out.indices) out[i] = bb.float
        return out
    }

    fun l2normalize(v: FloatArray): FloatArray {
        var s = 0.0
        for (f in v) s += f.toDouble() * f
        val norm = sqrt(s)
        if (norm <= 0.0 || norm.isNaN()) return v
        val out = FloatArray(v.size)
        for (i in v.indices) out[i] = (v[i] / norm.toFloat())
        return out
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return -1f
        var dot = 0.0
        for (i in a.indices) dot += a[i].toDouble() * b[i].toDouble()
        val na = l2norm(a)
        val nb = l2norm(b)
        if (na <= 0.0 || nb <= 0.0) return -1f
        return (dot / (na * nb)).toFloat()
    }

    private fun l2norm(v: FloatArray): Double {
        var s = 0.0
        for (f in v) s += f.toDouble() * f
        return sqrt(s)
    }
}
