package com.ailauncher.app.domain.wallpaper

import com.ailauncher.app.domain.models.WallpaperEntry
import com.ailauncher.app.domain.models.WallpaperMode
import com.ailauncher.app.domain.models.WallpaperSettings

/**
 * v10: which wallpaper shows right now, and how its adjustments become a color
 * matrix. The wallpaper settings screen existed since v5 but nothing rendered
 * it; this is the logic the new WallpaperLayer draws from. Pure Kotlin.
 */
object WallpaperRules {

    /**
     * The entry to draw, or null for "nothing of ours" (no entries — the theme
     * background shows — or LIVE mode, where the system wallpaper shows through).
     *
     * @param hour 0..23, @param dayOfWeek java.util.Calendar.DAY_OF_WEEK (1 = Sunday),
     * @param dayOfYear 1..366 — passed in so tests don't depend on the clock.
     */
    fun select(settings: WallpaperSettings, pageId: String, hour: Int, dayOfWeek: Int, dayOfYear: Int): WallpaperEntry? {
        val entries = settings.entries
        if (entries.isEmpty() || settings.mode == WallpaperMode.LIVE) return null
        return when (settings.mode) {
            WallpaperMode.SINGLE -> entries.first()
            WallpaperMode.PER_PAGE ->
                entries.firstOrNull { it.page == pageId } ?: entries.firstOrNull { it.page.isBlank() } ?: entries.first()
            WallpaperMode.HOURLY -> {
                val timed = entries.filter { it.hour in 0..23 }.sortedBy { it.hour }
                if (timed.isEmpty()) entries[hour % entries.size]
                // The latest start hour that has passed today; before the first one, the last from yesterday.
                else timed.lastOrNull { it.hour <= hour } ?: timed.last()
            }
            WallpaperMode.DAILY ->
                entries.firstOrNull { it.dayOfWeek == dayOfWeek } ?: entries[(dayOfYear - 1).coerceAtLeast(0) % entries.size]
            WallpaperMode.LIVE -> null
        }
    }

    /**
     * 4×5 row-major color matrix (android.graphics.ColorMatrix layout) applying,
     * in order: saturation (0 = grey, 1 = as-is, 2 = vivid), contrast (around
     * mid-grey), brightness (multiplier, 0..1) and warmth (-1 cool … +1 warm).
     */
    fun colorMatrix(entry: WallpaperEntry): FloatArray {
        val s = entry.saturation.coerceIn(0f, 2f)
        val c = entry.contrast.coerceIn(0f, 2f)
        val b = entry.brightness.coerceIn(0f, 1.5f)
        val w = entry.warmth.coerceIn(-1f, 1f)

        // Saturation (Rec. 709 luma weights, as in ColorMatrix.setSaturation).
        val lr = 0.2126f * (1 - s)
        val lg = 0.7152f * (1 - s)
        val lb = 0.0722f * (1 - s)
        val sat = arrayOf(
            floatArrayOf(lr + s, lg, lb),
            floatArrayOf(lr, lg + s, lb),
            floatArrayOf(lr, lg, lb + s)
        )
        // Contrast then brightness: out = (in * c + 128 * (1 - c)) * b, then warmth offsets.
        val scale = c * b
        val offset = 128f * (1 - c) * b
        val warm = floatArrayOf(30f * w, 8f * w, -30f * w)
        val m = FloatArray(20)
        for (row in 0..2) {
            for (col in 0..2) m[row * 5 + col] = sat[row][col] * scale
            m[row * 5 + 4] = offset + warm[row]
        }
        m[18] = 1f // alpha passes through
        return m
    }
}
