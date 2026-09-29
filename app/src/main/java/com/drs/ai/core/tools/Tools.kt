package com.drs.ai.core.tools

import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Seven fully-local tools. All pure Kotlin, JVM-testable, zero network.
 */
object Tools {

    // ---- 1. Calculator (recursive-descent parser) --------------------------

    class CalcException(message: String) : Exception(message)

    class Calculator(private val expr: String) {
        private var pos = 0

        fun evaluate(): Double {
            val v = parseExpression()
            if (pos < expr.length) throw CalcException("Unexpected '${expr[pos]}' at $pos")
            if (v.isNaN() || v.isInfinite()) throw CalcException("Result is not a finite number")
            return v
        }

        private fun skipWs() { while (pos < expr.length && expr[pos].isWhitespace()) pos++ }

        private fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                skipWs()
                if (pos < expr.length && (expr[pos] == '+' || expr[pos] == '-')) {
                    val op = expr[pos++]
                    val r = parseTerm()
                    v = if (op == '+') v + r else v - r
                } else return v
            }
        }

        private fun parseTerm(): Double {
            var v = parsePower()
            while (true) {
                skipWs()
                if (pos < expr.length && (expr[pos] == '*' || expr[pos] == '/' || expr[pos] == '%')) {
                    val op = expr[pos++]
                    val r = parsePower()
                    v = when (op) {
                        '*' -> v * r
                        '/' -> if (r == 0.0) throw CalcException("Division by zero") else v / r
                        else -> if (r == 0.0) throw CalcException("Modulo by zero") else v % r
                    }
                } else return v
            }
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            skipWs()
            if (pos < expr.length && expr[pos] == '^') {
                pos++
                return base.pow(parsePower())
            }
            return base
        }

        private fun parseUnary(): Double {
            skipWs()
            if (pos < expr.length && expr[pos] == '-') { pos++; return -parseUnary() }
            if (pos < expr.length && expr[pos] == '+') { pos++; return parseUnary() }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            skipWs()
            if (pos >= expr.length) throw CalcException("Unexpected end of expression")
            if (expr[pos] == '(') {
                pos++
                val v = parseExpression()
                skipWs()
                if (pos >= expr.length || expr[pos] != ')') throw CalcException("Missing ')'")
                pos++
                return v
            }
            val fn = listOf("sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "log10", "log", "ln", "abs", "exp").firstOrNull { expr.startsWith(it, pos, ignoreCase = true) }
            if (fn != null) {
                pos += fn.length
                skipWs()
                if (pos >= expr.length || expr[pos] != '(') throw CalcException("Expected '(' after $fn")
                pos++
                val arg = parseExpression()
                skipWs()
                if (pos >= expr.length || expr[pos] != ')') throw CalcException("Missing ')' after $fn")
                pos++
                return when (fn.lowercase()) {
                    "sqrt" -> if (arg < 0) throw CalcException("sqrt of negative") else sqrt(arg)
                    "sin" -> sin(Math.toRadians(arg))
                    "cos" -> cos(Math.toRadians(arg))
                    "tan" -> tan(Math.toRadians(arg))
                    "asin" -> asin(arg)
                    "acos" -> acos(arg)
                    "atan" -> atan(arg)
                    "log10" -> log10(arg)
                    "log" -> log10(arg)
                    "ln" -> if (arg <= 0) throw CalcException("ln of non-positive") else ln(arg)
                    "abs" -> abs(arg)
                    "exp" -> exp(arg)
                    else -> throw CalcException("Unknown function $fn")
                }
            }
            val const = when {
                expr.startsWith("pi", pos, ignoreCase = true) -> { pos += 2; return Math.PI }
                expr.startsWith("e", pos, ignoreCase = true) -> { pos += 1; return Math.E }
                else -> null
            }
            val start = pos
            while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) pos++
            if (pos == start) throw CalcException("Unexpected '${expr[pos]}' at $pos")
            val num = expr.substring(start, pos).toDoubleOrNull() ?: throw CalcException("Bad number '${expr.substring(start, pos)}'")
            return num
        }
    }

    fun calculate(expr: String): String {
        val v = Calculator(expr).evaluate()
        return if (v == Math.floor(v) && !v.isInfinite() && kotlin.math.abs(v) < 1e15) v.toLong().toString()
        else v.toString()
    }

    // ---- 2. Unit converter (to-base / from-base lambdas) -------------------

    data class UnitDef(val id: String, val label: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double)

    val categories: Map<String, List<UnitDef>> = mapOf(
        "length" to listOf(
            UnitDef("mm", "Millimeter", { it }, { it }),
            UnitDef("cm", "Centimeter", { it / 10.0 }, { it * 10.0 }),
            UnitDef("m", "Meter", { it }, { it }),
            UnitDef("km", "Kilometer", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("in", "Inch", { it * 0.0254 }, { it / 0.0254 }),
            UnitDef("ft", "Foot", { it * 0.3048 }, { it / 0.3048 }),
            UnitDef("mi", "Mile", { it * 1609.344 }, { it / 1609.344 })
        ),
        "mass" to listOf(
            UnitDef("g", "Gram", { it }, { it }),
            UnitDef("kg", "Kilogram", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("t", "Tonne", { it * 1_000_000.0 }, { it / 1_000_000.0 }),
            UnitDef("oz", "Ounce", { it * 28.349523125 }, { it / 28.349523125 }),
            UnitDef("lb", "Pound", { it * 453.59237 }, { it / 453.59237 })
        ),
        "temperature" to listOf(
            UnitDef("C", "Celsius", { it }, { it }),
            UnitDef("F", "Fahrenheit", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
            UnitDef("K", "Kelvin", { it - 273.15 }, { it + 273.15 })
        ),
        "area" to listOf(
            UnitDef("m2", "Square meter", { it }, { it }),
            UnitDef("km2", "Square kilometer", { it * 1e6 }, { it / 1e6 }),
            UnitDef("ha", "Hectare", { it * 1e4 }, { it / 1e4 }),
            UnitDef("ft2", "Square foot", { it * 0.09290304 }, { it / 0.09290304 }),
            UnitDef("acre", "Acre", { it * 4046.8564224 }, { it / 4046.8564224 })
        ),
        "volume" to listOf(
            UnitDef("ml", "Milliliter", { it }, { it }),
            UnitDef("l", "Liter", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("m3", "Cubic meter", { it * 1_000_000.0 }, { it / 1_000_000.0 }),
            UnitDef("gal", "US gallon", { it * 3785.411784 }, { it / 3785.411784 }),
            UnitDef("cup", "US cup", { it * 236.5882365 }, { it / 236.5882365 })
        ),
        "speed" to listOf(
            UnitDef("mps", "Meter/second", { it }, { it }),
            UnitDef("kmh", "Kilometer/hour", { it / 3.6 }, { it * 3.6 }),
            UnitDef("mph", "Mile/hour", { it * 0.44704 }, { it / 0.44704 }),
            UnitDef("knot", "Knot", { it * 0.514444 }, { it / 0.514444 })
        ),
        "data" to listOf(
            UnitDef("b", "Byte", { it }, { it }),
            UnitDef("kb", "Kilobyte", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("kib", "Kibibyte", { it * 1024.0 }, { it / 1024.0 }),
            UnitDef("mb", "Megabyte", { it * 1e6 }, { it / 1e6 }),
            UnitDef("mib", "Mebibyte", { it * 1048576.0 }, { it / 1048576.0 }),
            UnitDef("gb", "Gigabyte", { it * 1e9 }, { it / 1e9 }),
            UnitDef("gib", "Gibibyte", { it * 1073741824.0 }, { it / 1073741824.0 })
        ),
        "time" to listOf(
            UnitDef("s", "Second", { it }, { it }),
            UnitDef("min", "Minute", { it * 60.0 }, { it / 60.0 }),
            UnitDef("h", "Hour", { it * 3600.0 }, { it / 3600.0 }),
            UnitDef("d", "Day", { it * 86400.0 }, { it / 86400.0 }),
            UnitDef("wk", "Week", { it * 604800.0 }, { it / 604800.0 })
        ),
        "pressure" to listOf(
            UnitDef("pa", "Pascal", { it }, { it }),
            UnitDef("kpa", "Kilopascal", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("bar", "Bar", { it * 100000.0 }, { it / 100000.0 }),
            UnitDef("atm", "Atmosphere", { it * 101325.0 }, { it / 101325.0 }),
            UnitDef("psi", "PSI", { it * 6894.757293168 }, { it / 6894.757293168 })
        ),
        "energy" to listOf(
            UnitDef("j", "Joule", { it }, { it }),
            UnitDef("kj", "Kilojoule", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("cal", "Calorie", { it * 4.184 }, { it / 4.184 }),
            UnitDef("kcal", "Kilocalorie", { it * 4184.0 }, { it / 4184.0 }),
            UnitDef("wh", "Watt-hour", { it * 3600.0 }, { it / 3600.0 }),
            UnitDef("kwh", "Kilowatt-hour", { it * 3_600_000.0 }, { it / 3_600_000.0 })
        )
    )

    fun convert(category: String, fromId: String, toId: String, value: Double): Double {
        val units = categories[category] ?: throw CalcException("Unknown category $category")
        val from = units.firstOrNull { it.id == fromId } ?: throw CalcException("Unknown unit $fromId")
        val to = units.firstOrNull { it.id == toId } ?: throw CalcException("Unknown unit $toId")
        return to.fromBase(from.toBase(value))
    }

    // ---- 3. JSON formatter --------------------------------------------------

    object JsonTool {
        fun pretty(input: String): String {
            val el = kotlinx.serialization.json.Json.parseToJsonElement(input)
            return kotlinx.serialization.json.Json { prettyPrint = true; isLenient = true }.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(), el
            )
        }
        fun minify(input: String): String {
            val el = kotlinx.serialization.json.Json.parseToJsonElement(input)
            return el.toString()
        }
        fun validate(input: String): Boolean = try {
            kotlinx.serialization.json.Json.parseToJsonElement(input); true
        } catch (_: Exception) { false }
    }

    // ---- 4. Regex tester -----------------------------------------------------

    object RegexTool {
        data class Match(val start: Int, val end: Int, val text: String, val groups: List<String?>)
        fun test(pattern: String, input: String): List<Match> {
            val r = Regex(pattern)
            val out = mutableListOf<Match>()
            for (m in r.findAll(input)) {
                out.add(Match(m.range.first, m.range.last, m.value, m.groupValues.drop(1).map { g -> if (g.isEmpty()) null else g }))
            }
            return out
        }
    }

    // ---- 5. Base64 ------------------------------------------------------------

    object Base64Tool {
        fun encode(text: String): String = java.util.Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        fun decode(text: String): String = java.util.Base64.getMimeDecoder().decode(text.trim()).toString(Charsets.UTF_8)
    }

    // ---- 6. Hashes -------------------------------------------------------------

    object HashTool {
        fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
        fun digest(algorithm: String, input: String): String {
            val md = MessageDigest.getInstance(algorithm)
            return hex(md.digest(input.toByteArray(Charsets.UTF_8)))
        }
        fun all(input: String): Map<String, String> = mapOf(
            "MD5" to digest("MD5", input),
            "SHA-1" to digest("SHA-1", input),
            "SHA-256" to digest("SHA-256", input),
            "SHA-512" to digest("SHA-512", input)
        )
    }

    // ---- 7. UUID ----------------------------------------------------------------

    object UuidTool {
        fun generate(count: Int): List<String> {
            val n = count.coerceIn(1, 100)
            return List(n) { UUID.randomUUID().toString() }
        }
    }
}
