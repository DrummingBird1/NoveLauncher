package com.ailauncher.app.domain.calendar

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Robolectric because JewishCalendar uses android.icu (real ICU4J under
 * Robolectric). Expected dates are the published Israeli calendar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class JewishCalendarTest {

    private fun on(y: Int, m: Int, d: Int, diaspora: Boolean = false) =
        JewishCalendar.holidaysOn(LocalDate.of(y, m, d), diaspora)

    @Test
    fun `high holidays 5787`() {
        assertTrue(JewishHoliday.ROSH_HASHANA in on(2026, 9, 12))
        assertTrue(JewishHoliday.ROSH_HASHANA in on(2026, 9, 13))
        assertTrue(JewishHoliday.YOM_KIPPUR in on(2026, 9, 21))
        assertTrue(JewishHoliday.SUKKOT in on(2026, 9, 26))
    }

    @Test
    fun `Hanukkah lasts eight days across the Kislev-Tevet boundary`() {
        assertFalse(JewishHoliday.HANUKKAH in on(2026, 12, 4))
        assertTrue(JewishHoliday.HANUKKAH in on(2026, 12, 5))
        assertTrue(JewishHoliday.HANUKKAH in on(2026, 12, 12))
        assertFalse(JewishHoliday.HANUKKAH in on(2026, 12, 13))
    }

    @Test
    fun `spring holidays 5786`() {
        assertTrue(JewishHoliday.PURIM in on(2026, 3, 3))
        assertTrue(JewishHoliday.PESACH in on(2026, 4, 2))
        assertTrue(JewishHoliday.SHVII_SHEL_PESACH in on(2026, 4, 8))
        assertTrue(JewishHoliday.SHAVUOT in on(2026, 5, 22))
    }

    @Test
    fun `Independence Day moves back from Friday (2025) and stays put on Wednesday (2026)`() {
        assertTrue(JewishHoliday.YOM_HAATZMAUT in on(2025, 5, 1))
        assertTrue(JewishHoliday.YOM_HAZIKARON in on(2025, 4, 30))
        assertFalse(JewishHoliday.YOM_HAATZMAUT in on(2025, 5, 2))

        assertTrue(JewishHoliday.YOM_HAATZMAUT in on(2026, 4, 22))
        assertTrue(JewishHoliday.YOM_HAZIKARON in on(2026, 4, 21))
    }

    @Test
    fun `Tisha B'Av on Shabbat is observed on Sunday`() {
        assertFalse(JewishHoliday.TISHA_BAV in on(2025, 8, 2))
        assertTrue(JewishHoliday.TISHA_BAV in on(2025, 8, 3))
    }

    @Test
    fun `diaspora adds the second Yom Tov day`() {
        assertFalse(JewishHoliday.PESACH in on(2026, 4, 3))
        assertTrue(JewishHoliday.PESACH in on(2026, 4, 3, diaspora = true))
        assertTrue(JewishHoliday.SIMCHAT_TORAH in on(2026, 10, 3)) // 22 Tishrei, Israel
        assertTrue(JewishHoliday.SIMCHAT_TORAH in on(2026, 10, 4, diaspora = true))
    }

    @Test
    fun `leap years follow the 19-year cycle`() {
        assertFalse(JewishCalendar.isLeapYear(5786))
        assertTrue(JewishCalendar.isLeapYear(5787))
        assertTrue(JewishCalendar.isLeapYear(5784))
    }

    @Test
    fun `Hebrew date formats in the requested language`() {
        val en = JewishCalendar.format(LocalDate.of(2026, 4, 2), Locale.ENGLISH)
        assertTrue(en, en.contains("Nisan") && en.contains("5786"))
        val he = JewishCalendar.format(LocalDate.of(2026, 4, 2), Locale.forLanguageTag("he"))
        assertTrue(he, he.contains("ניסן"))
    }

    @Test
    fun `Shabbat mode window runs from candle lighting to nightfall`() {
        val zone = ZoneId.of("Asia/Jerusalem")
        val lat = 32.0853
        val lon = 34.7818
        val friday = LocalDate.of(2026, 10, 16)
        val saturday = friday.plusDays(1)
        val fridaySunset = SolarCalculator.sunset(friday, lat, lon, zone)!!
        val saturdayTzeit = SolarCalculator.sunset(saturday, lat, lon, zone, SolarCalculator.ZENITH_TZEIT)!!
        fun rest(t: Long) = JewishCalendar.isRestPeriodNow(t, zone, lat, lon, candleLightingMinutes = 18)

        assertFalse(rest(fridaySunset - 30 * 60_000L))
        assertTrue(rest(fridaySunset - 10 * 60_000L))
        assertTrue(rest(saturday.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()))
        assertTrue(rest(saturdayTzeit - 60_000L))
        assertFalse(rest(saturdayTzeit + 60_000L))
    }

    @Test
    fun `day info reports candle lighting on Friday and havdalah on Saturday`() {
        val zone = ZoneId.of("Asia/Jerusalem")
        val noon = { d: LocalDate -> d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli() }
        val friday = JewishCalendar.dayInfo(noon(LocalDate.of(2026, 10, 16)), zone, 32.0853, 34.7818, Locale.ENGLISH, 18)
        val saturday = JewishCalendar.dayInfo(noon(LocalDate.of(2026, 10, 17)), zone, 32.0853, 34.7818, Locale.ENGLISH, 18)
        assertTrue(friday.candleLighting != null && friday.havdalah == null)
        assertTrue(saturday.havdalah != null && saturday.candleLighting == null)
        assertTrue(saturday.isRestPeriodNow)
        assertFalse(friday.isRestPeriodNow)
    }
}
