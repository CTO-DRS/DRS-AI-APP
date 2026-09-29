package com.drs.ai.core.rag

/**
 * Sliding-window chunker with sentence-boundary snapping. JVM-testable, no Android deps.
 */
object Chunker {

    fun chunk(text: String, maxLen: Int = 512, overlap: Int = 64): List<String> {
        val clean = text.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ").trim()
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= maxLen) return listOf(clean)

        val out = mutableListOf<String>()
        var start = 0
        val sentences = splitSentences(clean)
        var current = StringBuilder()

        fun flush() {
            val s = current.toString().trim()
            if (s.isNotEmpty()) out.add(s)
            current = StringBuilder()
        }

        for (sentence in sentences) {
            if (sentence.length > maxLen) {
                flush()
                // hard-split oversized sentence with overlap
                var i = 0
                while (i < sentence.length) {
                    out.add(sentence.substring(i, (i + maxLen).coerceAtMost(sentence.length)))
                    i += maxLen - overlap
                    if (i < 0) break
                }
                continue
            }
            if (current.length + sentence.length + 1 > maxLen) flush()
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        flush()
        return out
    }

    fun splitSentences(text: String): List<String> {
        val parts = text.split(Regex("(?<=[.!?؟।\n])\\s+"))
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
