package com.ailauncher.app.domain.wallpaper

import com.ailauncher.app.domain.models.WallpaperEntry
import com.ailauncher.app.domain.models.WallpaperMode
import com.ailauncher.app.domain.models.WallpaperSettings
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class WallpaperRulesTest {

    private val a = WallpaperEntry(id = "a")
    private val b = WallpaperEntry(id = "b")
    private val c = WallpaperEntry(id = "c")

    private fun pick(settings: WallpaperSettings, page: String = "home", hour: Int = 12, dow: Int = Calendar.MONDAY, doy: Int = 1) =
        WallpaperRules.select(settings, page, hour, dow, doy)?.id

    @Test
    fun `nothing to draw without entries or in live mode`() {
        assertNull(pick(WallpaperSettings()))
        assertNull(pick(WallpaperSettings(mode = WallpaperMode.LIVE, entries = listOf(a))))
    }

    @Test
    fun `single uses the first entry`() {
        assertEquals("a", pick(WallpaperSettings(mode = WallpaperMode.SINGLE, entries = listOf(a, b))))
    }

    @Test
    fun `per page falls back to the any-page entry`() {
        val s = WallpaperSettings(mode = WallpaperMode.PER_PAGE, entries = listOf(a.copy(page = "apps"), b, c.copy(page = "home")))
        assertEquals("c", pick(s, page = "home"))
        assertEquals("a", pick(s, page = "apps"))
        assertEquals("b", pick(s, page = "news"))
    }

    @Test
    fun `hourly picks the latest start hour that has passed, wrapping overnight`() {
        val s = WallpaperSettings(mode = WallpaperMode.HOURLY, entries = listOf(a.copy(hour = 6), b.copy(hour = 18)))
        assertEquals("a", pick(s, hour = 6))
        assertEquals("a", pick(s, hour = 17))
        assertEquals("b", pick(s, hour = 18))
        assertEquals("b", pick(s, hour = 3)) // before 06:00 → still the evening one
    }

    @Test
    fun `hourly without start hours rotates`() {
        val s = WallpaperSettings(mode = WallpaperMode.HOURLY, entries = listOf(a, b))
        assertEquals("a", pick(s, hour = 10))
        assertEquals("b", pick(s, hour = 11))
    }

    @Test
    fun `daily matches the weekday, else rotates by day of year`() {
        val s = WallpaperSettings(mode = WallpaperMode.DAILY, entries = listOf(a.copy(dayOfWeek = Calendar.FRIDAY), b, c))
        assertEquals("a", pick(s, dow = Calendar.FRIDAY))
        assertEquals("b", pick(s, dow = Calendar.MONDAY, doy = 2))
    }

    @Test
    fun `default adjustments produce the identity color matrix`() {
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        assertArrayEquals(identity, WallpaperRules.colorMatrix(WallpaperEntry()), 0.0001f)
    }

    @Test
    fun `zero saturation makes every channel the same luma mix`() {
        val m = WallpaperRules.colorMatrix(WallpaperEntry(saturation = 0f))
        for (row in 0..2) {
            assertEquals(0.2126f, m[row * 5], 0.0001f)
            assertEquals(0.7152f, m[row * 5 + 1], 0.0001f)
            assertEquals(0.0722f, m[row * 5 + 2], 0.0001f)
        }
    }

    @Test
    fun `warmth pushes red up and blue down`() {
        val m = WallpaperRules.colorMatrix(WallpaperEntry(warmth = 1f))
        assertEquals(30f, m[4], 0.0001f)
        assertEquals(-30f, m[14], 0.0001f)
    }
}
