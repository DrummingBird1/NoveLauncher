package com.ailauncher.app.data.ml

import com.ailauncher.app.data.db.HourlyUsageEntity
import com.ailauncher.app.data.db.UsageCacheEntity
import com.ailauncher.app.data.db.UsageDao
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Pure-function tests for AppPredictionEngine.extractFeatures/computeFeatureScore —
 * the feature extraction + weighted scoring that's meant to stay stable if/when
 * computeFeatureScore's body is swapped for a TFLite model (see class kdoc).
 */
class AppPredictionEngineTest {

    private lateinit var engine: AppPredictionEngine

    @Before fun setUp() {
        engine = AppPredictionEngine(mockk(relaxed = true), mockk<UsageDao>(relaxed = true))
    }

    private fun usage(pkg: String, lastUsed: Long, launches: Int, foregroundMs: Long = 0) =
        UsageCacheEntity(packageName = pkg, totalTimeInForeground = foregroundMs, lastTimeUsed = lastUsed, launchCount = launches)

    @Test fun `recency score decays with hours since last use`() {
        val now = System.currentTimeMillis()
        val recent = usage("a", now - 3_600_000L, 5) // 1 hour ago
        val old = usage("b", now - 20 * 3_600_000L, 5) // 20 hours ago

        val recentFeature = engine.extractFeatures(recent, emptyList(), currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)
        val oldFeature = engine.extractFeatures(old, emptyList(), currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)

        val recentScore = engine.computeFeatureScore(recentFeature)
        val oldScore = engine.computeFeatureScore(oldFeature)
        assertTrue("recent ($recentScore) should outscore old ($oldScore)", recentScore > oldScore)
    }

    @Test fun `never-used app scores no higher than the day-of-week baseline`() {
        // Like AppCategoryProvider's scoring, dayOfWeekMatch (weekday=0.8) is a pure
        // calendar signal, not usage-dependent — so a never-used app doesn't land at
        // exactly 0, it's bounded by W_DAY_MATCH * 0.8 = 0.10 * 0.8 = 0.08.
        val now = System.currentTimeMillis()
        val neverUsed = usage("a", 0L, 0)
        val feature = engine.extractFeatures(neverUsed, emptyList(), currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)
        val score = engine.computeFeatureScore(feature)
        assertTrue("never-used app should stay in the low baseline band, was $score", score < 0.15f)
    }

    @Test fun `hour-of-day match rewards apps used near the current hour`() {
        val now = System.currentTimeMillis()
        val u = usage("a", now - 3_600_000L, 10)
        // All historical launches clustered at hour 9 — current hour 9 should match,
        // current hour 21 (12h away) should not.
        val hourly = listOf(HourlyUsageEntity(packageName = "a", hour = 9, count = 10))

        val matching = engine.extractFeatures(u, hourly, currentHour = 9, currentDay = Calendar.WEDNESDAY, now = now)
        val nonMatching = engine.extractFeatures(u, hourly, currentHour = 21, currentDay = Calendar.WEDNESDAY, now = now)

        assertEquals(1f, matching.hourOfDayMatch, 0.01f)
        assertEquals(0f, nonMatching.hourOfDayMatch, 0.01f)
        assertTrue(engine.computeFeatureScore(matching) > engine.computeFeatureScore(nonMatching))
    }

    @Test fun `hour match also counts the adjacent hour on each side`() {
        val now = System.currentTimeMillis()
        val u = usage("a", now, 10)
        val hourly = listOf(HourlyUsageEntity(packageName = "a", hour = 9, count = 10))

        // currentHour 10 is adjacent to the 9am cluster (9,10,11 all count as "nearby").
        val adjacent = engine.extractFeatures(u, hourly, currentHour = 10, currentDay = Calendar.WEDNESDAY, now = now)
        assertEquals(1f, adjacent.hourOfDayMatch, 0.01f)
    }

    @Test fun `weekend day-of-week match uses the Hebrew weekend, Friday and Saturday`() {
        val now = System.currentTimeMillis()
        val u = usage("a", now, 1)

        val friday = engine.extractFeatures(u, emptyList(), currentHour = 12, currentDay = Calendar.FRIDAY, now = now)
        val saturday = engine.extractFeatures(u, emptyList(), currentHour = 12, currentDay = Calendar.SATURDAY, now = now)
        val sunday = engine.extractFeatures(u, emptyList(), currentHour = 12, currentDay = Calendar.SUNDAY, now = now)
        val wednesday = engine.extractFeatures(u, emptyList(), currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)

        assertEquals(0.5f, friday.dayOfWeekMatch, 0.01f)
        assertEquals(0.5f, saturday.dayOfWeekMatch, 0.01f)
        assertEquals(0.8f, sunday.dayOfWeekMatch, 0.01f)
        assertEquals(0.8f, wednesday.dayOfWeekMatch, 0.01f)
    }

    @Test fun `frequency uses log scaling so it does not dominate a fresh install`() {
        val now = System.currentTimeMillis()
        val fewLaunches = usage("a", now, 2)
        val manyLaunches = usage("b", now, 200)

        val fewFeature = engine.extractFeatures(fewLaunches, emptyList(), currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)
        val manyFeature = engine.extractFeatures(manyLaunches, emptyList(), currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)

        assertTrue(manyFeature.frequencyLog > fewFeature.frequencyLog)
        // Log scaling means 100x the launches must NOT translate to 100x the feature value.
        assertTrue(manyFeature.frequencyLog < fewFeature.frequencyLog * 100)
    }

    @Test fun `computeFeatureScore stays clamped to the 0 to 1 range`() {
        val now = System.currentTimeMillis()
        val heavyUse = usage("a", now, 10_000, foregroundMs = 100_000_000L)
        val hourly = listOf(HourlyUsageEntity(packageName = "a", hour = 12, count = 10_000))
        val feature = engine.extractFeatures(heavyUse, hourly, currentHour = 12, currentDay = Calendar.WEDNESDAY, now = now)
        val score = engine.computeFeatureScore(feature)
        assertTrue("score=$score must stay within [0,1]", score in 0f..1f)
    }
}
