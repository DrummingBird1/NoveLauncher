package com.ailauncher.app.domain.search

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.pow

/**
 * v10: instant answers in Global Search — arithmetic ("12*7+3", "15% of 80",
 * "2^10") and unit conversion ("10 km to mi", "100 f in c", "5 ק"ג לפאונד").
 * Entirely offline; anything that doesn't parse cleanly returns null and the
 * search simply shows no answer card.
 */
object QuickAnswers {
    data class Answer(
        val display: String,
        val copyValue: String,
    )

    fun answer(query: String): Answer? = convertUnits(query) ?: calculate(query)

    // ── Calculator ─────────────────────────────────────────────────────

    fun calculate(input: String): Answer? {
        val expr =
            input
                .trim()
                .replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-')
                .replace(',', '.')
                .removeSuffix("=")
                .trim()
        if (expr.isEmpty() || expr.length > 120) return null
        if (!Regex("""^[0-9.\s()+\-*/^%of]+$""").matches(expr)) return null
        // Needs a binary operator — a bare number isn't a calculation — and a
        // phone number like 054-1234567 shouldn't come back as a subtraction.
        val hasOperator = Regex("""[0-9.)%]\s*[-+*/^]""").containsMatchIn(expr) || Regex("""%\s*of""").containsMatchIn(expr)
        if (!hasOperator) return null
        if (Regex("""^0\d[\d\s-]{6,}$""").matches(expr)) return null
        val value =
            try {
                Parser(expr).parse()
            } catch (_: Exception) {
                return null
            }
        if (value.isNaN() || value.isInfinite()) return null
        val text = format(value)
        return Answer("= $text", text)
    }

    /** Recursive-descent parser: + - * / ^ %, parentheses, unary minus, "X% of Y". */
    private class Parser(
        private val s: String,
    ) {
        private var pos = 0

        fun parse(): Double {
            val v = expression()
            skipWs()
            require(pos == s.length) { "trailing input" }
            return v
        }

        private fun expression(): Double {
            var v = term()
            while (true) {
                skipWs()
                v =
                    when (peek()) {
                        '+' -> {
                            pos++
                            v + term()
                        }

                        '-' -> {
                            pos++
                            v - term()
                        }

                        else -> {
                            return v
                        }
                    }
            }
        }

        private fun term(): Double {
            var v = power()
            while (true) {
                skipWs()
                v =
                    when (peek()) {
                        '*' -> {
                            pos++
                            v * power()
                        }

                        '/' -> {
                            pos++
                            val d = power()
                            require(d != 0.0)
                            v / d
                        }

                        else -> {
                            return v
                        }
                    }
            }
        }

        private fun power(): Double {
            val base = unary()
            skipWs()
            return if (peek() == '^') {
                pos++
                base.pow(power())
            } else {
                base
            }
        }

        private fun unary(): Double {
            skipWs()
            return when (peek()) {
                '-' -> {
                    pos++
                    -unary()
                }

                '+' -> {
                    pos++
                    unary()
                }

                else -> {
                    percent()
                }
            }
        }

        private fun percent(): Double {
            var v = primary()
            skipWs()
            if (peek() == '%') {
                pos++
                skipWs()
                if (s.startsWith("of", pos)) {
                    pos += 2
                    v = v / 100.0 * unary()
                } else {
                    v /= 100.0
                }
            }
            return v
        }

        private fun primary(): Double {
            skipWs()
            if (peek() == '(') {
                pos++
                val v = expression()
                skipWs()
                require(peek() == ')')
                pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start)
            return s.substring(start, pos).toDouble()
        }

        private fun peek(): Char? = s.getOrNull(pos)

        private fun skipWs() {
            while (pos < s.length && s[pos] == ' ') pos++
        }
    }

    // ── Unit conversion ────────────────────────────────────────────────

    private enum class Dim { LENGTH, MASS, VOLUME, SPEED, TEMPERATURE }

    /** Linear units: factor to the dimension's base unit (m, kg, L, m/s). */
    private data class Unit(
        val symbol: String,
        val dim: Dim,
        val factor: Double,
    )

    private val UNITS: Map<String, Unit> =
        buildMap {
            fun u(
                symbol: String,
                dim: Dim,
                factor: Double,
                vararg aliases: String,
            ) {
                val unit = Unit(symbol, dim, factor)
                (listOf(symbol) + aliases).forEach { put(it, unit) }
            }
            u("mm", Dim.LENGTH, 0.001, "millimeter", "millimeters", "מ\"מ", "ממ", "מילימטר")
            u("cm", Dim.LENGTH, 0.01, "centimeter", "centimeters", "ס\"מ", "סמ", "סנטימטר")
            u("m", Dim.LENGTH, 1.0, "meter", "meters", "metre", "metres", "מטר", "מטרים", "מ")
            u("km", Dim.LENGTH, 1000.0, "kilometer", "kilometers", "kilometre", "ק\"מ", "קמ", "קילומטר", "קילומטרים")
            u("in", Dim.LENGTH, 0.0254, "inch", "inches", "\"", "אינץ", "אינץ'")
            u("ft", Dim.LENGTH, 0.3048, "foot", "feet", "'", "רגל", "פיט")
            u("yd", Dim.LENGTH, 0.9144, "yard", "yards", "יארד")
            u("mi", Dim.LENGTH, 1609.344, "mile", "miles", "מייל", "מיילים")
            u("g", Dim.MASS, 0.001, "gram", "grams", "גרם", "ג")
            u("kg", Dim.MASS, 1.0, "kilogram", "kilograms", "kilo", "kilos", "ק\"ג", "קג", "קילו", "קילוגרם")
            u("lb", Dim.MASS, 0.45359237, "lbs", "pound", "pounds", "פאונד", "ליברה")
            u("oz", Dim.MASS, 0.028349523125, "ounce", "ounces", "אונקיה", "אונקיות")
            u("ml", Dim.VOLUME, 0.001, "milliliter", "milliliters", "מ\"ל", "מל", "מיליליטר")
            u("l", Dim.VOLUME, 1.0, "liter", "liters", "litre", "litres", "ליטר", "ליטרים")
            u("gal", Dim.VOLUME, 3.785411784, "gallon", "gallons", "גלון", "גלונים")
            u("cup", Dim.VOLUME, 0.2365882365, "cups", "כוס", "כוסות")
            u("km/h", Dim.SPEED, 1 / 3.6, "kmh", "kph", "קמ\"ש", "קמש")
            u("mph", Dim.SPEED, 0.44704, "מיילים לשעה")
            u("m/s", Dim.SPEED, 1.0, "mps")
            u("°C", Dim.TEMPERATURE, 1.0, "c", "celsius", "°c", "צלזיוס")
            u("°F", Dim.TEMPERATURE, 1.0, "f", "fahrenheit", "°f", "פרנהייט")
            u("K", Dim.TEMPERATURE, 1.0, "k", "kelvin", "קלווין")
        }

    // "<number> <unit> (to|in|=|->|ל|ב|…) <unit>". The Hebrew prepositions ל/ב are
    // usually written glued to the next word ("לפאונד"), hence `\s*` after them.
    private val CONVERSION =
        Regex(
            """^\s*(-?\d+(?:[.,]\d+)?)\s*(.+?)\s+(?:to|in|into|as|=|->|→|ל|ל-|ב|ב-|בתור)\s*(.+?)\s*\??\s*$""",
            RegexOption.IGNORE_CASE,
        )

    fun convertUnits(input: String): Answer? {
        val m = CONVERSION.matchEntire(input) ?: return null
        val amount = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val from = lookup(m.groupValues[2]) ?: return null
        val to = lookup(m.groupValues[3]) ?: return null
        if (from.dim != to.dim || from == to) return null
        val result =
            if (from.dim == Dim.TEMPERATURE) {
                convertTemperature(amount, from.symbol, to.symbol)
            } else {
                amount * from.factor / to.factor
            }
        val text = format(result)
        return Answer("${format(amount)} ${from.symbol} = $text ${to.symbol}", text)
    }

    private fun lookup(token: String): Unit? {
        val t =
            token
                .trim()
                .lowercase()
                .replace('״', '"')
                .replace('׳', '\'')
        return UNITS[t] ?: UNITS[token.trim()]
    }

    private fun convertTemperature(
        v: Double,
        from: String,
        to: String,
    ): Double {
        val celsius =
            when (from) {
                "°F" -> (v - 32) * 5 / 9
                "K" -> v - 273.15
                else -> v
            }
        return when (to) {
            "°F" -> celsius * 9 / 5 + 32
            "K" -> celsius + 273.15
            else -> celsius
        }
    }

    /** Up to 10 significant digits, no trailing zeros, no scientific notation for everyday sizes. */
    internal fun format(v: Double): String {
        if (v == 0.0) return "0"
        val bd = BigDecimal(v).round(MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros()
        return if (bd.abs() >= BigDecimal("1e15") || bd.abs() < BigDecimal("1e-6")) bd.toString() else bd.toPlainString()
    }
}
