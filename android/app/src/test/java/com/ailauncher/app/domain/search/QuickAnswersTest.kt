package com.ailauncher.app.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickAnswersTest {
    private fun calc(expr: String) = QuickAnswers.calculate(expr)?.copyValue

    private fun convert(q: String) = QuickAnswers.convertUnits(q)?.copyValue

    @Test
    fun `arithmetic respects precedence and parentheses`() {
        assertEquals("87", calc("12*7+3"))
        assertEquals("20", calc("(2+3)*4"))
        assertEquals("2.5", calc("10/4"))
        assertEquals("1024", calc("2^10"))
        assertEquals("-6", calc("-2*3"))
        assertEquals("42", calc("6 × 7"))
        assertEquals("4", calc("8÷2="))
    }

    @Test
    fun `percent of`() {
        assertEquals("12", calc("15% of 80"))
        assertEquals("110", calc("100+10%*100"))
    }

    @Test
    fun `not a calculation`() {
        assertNull(calc("5"))
        assertNull(calc("-5"))
        assertNull(calc("hello"))
        assertNull(calc("1/0"))
        assertNull(calc("054-1234567")) // a phone number, not a subtraction
        assertNull(calc("2+"))
    }

    @Test
    fun `length, mass, volume and speed`() {
        assertEquals("6.213711922", convert("10 km to mi"))
        assertEquals("1000", convert("1 l in ml"))
        assertEquals("30.48", convert("1 ft to cm"))
        assertEquals("100", convert("62.13711922 mph to km/h")?.let { String.format("%.0f", it.toDouble()) })
    }

    @Test
    fun `temperature is affine, not a ratio`() {
        assertEquals("37.77777778", convert("100 f to c"))
        assertEquals("212", convert("100 c to f"))
        assertEquals("273.15", convert("0 c to k"))
    }

    @Test
    fun `Hebrew units and prepositions`() {
        assertEquals("11.02311311", convert("5 ק\"ג לפאונד"))
        assertEquals("1.609344", convert("1 מייל לק״מ"))
    }

    @Test
    fun `incompatible or unknown units give no answer`() {
        assertNull(convert("10 km to kg"))
        assertNull(convert("10 km to km"))
        assertNull(convert("10 parsecs to km"))
    }

    @Test
    fun `answer prefers a unit conversion over arithmetic`() {
        assertEquals("1000 m = 1 km", QuickAnswers.answer("1000 m to km")?.display)
        assertEquals("= 4", QuickAnswers.answer("2+2")?.display)
    }
}
