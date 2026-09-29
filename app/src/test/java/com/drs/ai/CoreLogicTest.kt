package com.drs.ai

import com.drs.ai.core.models.GgufParser
import com.drs.ai.core.rag.Chunker
import com.drs.ai.core.rag.VectorMath
import com.drs.ai.core.tools.Tools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * JVM unit tests for the pure-Kotlin core: chunker, vectors, tools, GGUF parser.
 */
class CoreLogicTest {

    // ---- Chunker ------------------------------------------------------------

    @Test
    fun chunkerRespectsMaxLength() {
        val text = ("lorem ipsum dolor sit amet ").repeat(200)
        val chunks = Chunker.chunk(text, maxLen = 512, overlap = 64)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.length <= 512 })
    }

    @Test
    fun chunkerSplitsOnSentences() {
        val text = "First sentence. Second sentence! Third question؟ Fourth line.\nFifth line."
        val chunks = Chunker.chunk(text, maxLen = 30, overlap = 5)
        assertTrue(chunks.joinToString(" ").contains("First sentence."))
    }

    @Test
    fun chunkerEmptyInput() {
        assertTrue(Chunker.chunk("   ").isEmpty())
    }

    // ---- VectorMath -----------------------------------------------------------

    @Test
    fun packUnpackRoundTrip() {
        val v = floatArrayOf(0.25f, -1.5f, 3.75f, 0f)
        val unpacked = VectorMath.unpack(VectorMath.pack(v))
        for (i in v.indices) assertEquals(v[i], unpacked[i], 1e-6f)
    }

    @Test
    fun cosineParallelVectorsIsOne() {
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(2f, 4f, 6f)
        assertEquals(1f, VectorMath.cosine(a, b), 1e-4f)
    }

    @Test
    fun cosineOrthogonalIsZero() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0f, VectorMath.cosine(a, b), 1e-4f)
    }

    @Test
    fun l2NormalizeProducesUnitVector() {
        val v = VectorMath.l2normalize(floatArrayOf(3f, 4f))
        assertEquals(0.6f, v[0], 1e-5f)
        assertEquals(0.8f, v[1], 1e-5f)
    }

    // ---- Calculator -------------------------------------------------------------

    @Test
    fun calculatorArithmetic() {
        assertEquals("8", Tools.calculate("2+2*3"))
        assertEquals("2", Tools.calculate("(2+2)/2"))
        assertEquals("4", Tools.calculate("2^2"))
        assertEquals("1024", Tools.calculate("2^10"))
    }

    @Test
    fun calculatorFunctions() {
        assertTrue(Tools.calculate("sqrt(16)").toDouble() == 4.0)
        assertTrue(Tools.calculate("abs(-5)").toDouble() == 5.0)
        assertTrue(Tools.calculate("log(100)").toDouble() == 2.0)
    }

    @Test
    fun calculatorDivisionByZeroThrows() {
        assertThrows(Tools.CalcException::class.java) { Tools.calculate("1/0") }
    }

    @Test
    fun calculatorInvalidThrows() {
        assertThrows(Tools.CalcException::class.java) { Tools.calculate("2+") }
        assertThrows(Tools.CalcException::class.java) { Tools.calculate("sin") }
    }

    // ---- Unit converter (temperature affine — regression for C→K bug) ---------

    @Test
    fun celsiusToKelvinIsAffine() {
        val k = Tools.convert("temperature", "C", "K", 25.0)
        assertEquals(298.15, k, 1e-6)
    }

    @Test
    fun celsiusToFahrenheit() {
        assertEquals(77.0, Tools.convert("temperature", "C", "F", 25.0), 1e-6)
    }

    @Test
    fun kelvinToCelsius() {
        assertEquals(0.0, Tools.convert("temperature", "K", "C", 273.15), 1e-6)
    }

    @Test
    fun kmToMiles() {
        assertEquals(0.621371192237334, Tools.convert("length", "km", "mi", 1.0), 1e-6)
        assertEquals(1.609344, Tools.convert("length", "mi", "km", 1.0), 1e-6)
    }

    // ---- JSON --------------------------------------------------------------------

    @Test
    fun jsonPrettyAndMinify() {
        val json = """{"a":1,"b":[2,3]}"""
        val pretty = Tools.JsonTool.pretty(json)
        assertTrue(pretty.contains("\n"))
        val min = Tools.JsonTool.minify(json)
        assertFalse(min.contains(' '))
        assertTrue(Tools.JsonTool.validate(json))
        assertFalse(Tools.JsonTool.validate("{bad"))
    }

    // ---- Regex / Base64 / Hash / UUID --------------------------------------------

    @Test
    fun regexFindsMatches() {
        val ms = Tools.RegexTool.test("\\d+", "a1 b22 c333")
        assertEquals(3, ms.size)
        assertEquals("333", ms[2].text)
    }

    @Test
    fun base64RoundTrip() {
        val src = "نص عربي 123"
        assertEquals(src, Tools.Base64Tool.decode(Tools.Base64Tool.encode(src)))
    }

    @Test
    fun hashKnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Tools.HashTool.digest("SHA-256", "abc"))
    }

    @Test
    fun uuidGeneration() {
        val ids = Tools.UuidTool.generate(5)
        assertEquals(5, ids.size)
        assertEquals(5, ids.toSet().size)
    }

    // ---- GGUF parser (real header constructed in temp file) ----------------------

    private fun writeGguf(): File {
        val f = File.createTempFile("test", ".gguf")
        val bb = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("GGUF".toByteArray(Charsets.US_ASCII))
        bb.putInt(3) // version
        bb.putLong(1) // tensor count
        bb.putLong(4) // kv count

        fun putStr(s: String) {
            bb.putLong(s.length.toLong())
            bb.put(s.toByteArray(Charsets.UTF_8))
        }
        fun putKvStr(key: String, value: String) {
            putStr(key)
            bb.putInt(8) // type string
            putStr(value)
        }
        fun putKvU32(key: String, value: Int) {
            putStr(key)
            bb.putInt(4) // u32
            bb.putInt(value)
        }
        putKvStr("general.architecture", "llama")
        putKvStr("general.name", "test-model")
        putKvU32("llama.context_length", 4096)
        putKvU32("llama.block_count", 22)

        val bytes = bb.array().copyOf(bb.position())
        f.writeBytes(bytes)
        return f
    }

    @Test
    fun ggufParserReadsHeader() {
        val f = writeGguf()
        try {
            assertTrue(GgufParser.isGguf(f))
            val info = GgufParser.parse(f)
            assertEquals("llama", info.arch)
            assertEquals("test-model", info.name)
            assertEquals(4096L, info.ctxLength)
            assertEquals(22L, info.blockCount)
            assertFalse(info.isMmproj)
        } finally {
            f.delete()
        }
    }

    @Test
    fun ggufRejectsNonGguf() {
        val f = File.createTempFile("bad", ".bin")
        f.writeBytes("not a model".toByteArray())
        try {
            assertFalse(GgufParser.isGguf(f))
            org.junit.Assert.assertThrows(GgufParser.GgufException::class.java) { GgufParser.parse(f) }
        } finally {
            f.delete()
        }
    }
}
