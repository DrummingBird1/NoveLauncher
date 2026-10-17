package com.ailauncher.app.domain.calendar

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class SolarCalculatorTest {
    private val israel = ZoneId.of("Asia/Jerusalem")

    private fun localTime(
        millis: Long,
        zone: ZoneId,
    ): LocalTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()

    private fun assertNear(
        expected: LocalTime,
        actual: LocalTime,
        toleranceMin: Int = 4,
    ) {
        val diff = abs(expected.toSecondOfDay() - actual.toSecondOfDay()) / 60
        assertTrue("expected ~$expected, got $actual", diff <= toleranceMin)
    }

    @Test
    fun `Jerusalem summer solstice sunset is about 19 48`() {
        val t = SolarCalculator.sunset(LocalDate.of(2026, 6, 21), 31.7683, 35.2137, israel)!!
        assertNear(LocalTime.of(19, 48), localTime(t, israel))
    }

    @Test
    fun `Tel Aviv winter solstice sunset is about 16 41`() {
        val t = SolarCalculator.sunset(LocalDate.of(2026, 12, 21), 32.0853, 34.7818, israel)!!
        assertNear(LocalTime.of(16, 41), localTime(t, israel))
    }

    @Test
    fun `Tel Aviv sunrise on the equinox is about 05 45 local winter time`() {
        val t = SolarCalculator.sunrise(LocalDate.of(2026, 3, 20), 32.0853, 34.7818, israel)!!
        assertNear(LocalTime.of(5, 45), localTime(t, israel))
    }

    @Test
    fun `nightfall comes after sunset`() {
        val d = LocalDate.of(2026, 10, 17)
        val sunset = SolarCalculator.sunset(d, 32.0853, 34.7818, israel)!!
        val tzeit = SolarCalculator.sunset(d, 32.0853, 34.7818, israel, SolarCalculator.ZENITH_TZEIT)!!
        val minutes = (tzeit - sunset) / 60_000
        assertTrue("tzeit should be ~30-45 min after sunset in Israel, was $minutes", minutes in 25..50)
    }

    @Test
    fun `the result falls on the requested local date far from Greenwich`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val d = LocalDate.of(2026, 6, 21)
        val t = SolarCalculator.sunset(d, 35.6762, 139.6503, tokyo)!!
        assertTrue(Instant.ofEpochMilli(t).atZone(tokyo).toLocalDate() == d)
        assertNear(LocalTime.of(19, 0), localTime(t, tokyo), toleranceMin = 6)
    }

    @Test
    fun `no sunset during polar day`() {
        assertNull(SolarCalculator.sunset(LocalDate.of(2026, 6, 21), 80.0, 15.0, ZoneId.of("UTC")))
    }
}
