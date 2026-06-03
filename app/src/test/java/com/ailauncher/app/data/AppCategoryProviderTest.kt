package com.ailauncher.app.data

import android.content.Context
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.UsageSnapshot
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure-function tests for AppCategoryProvider. The provider needs a Context for
 * package-info lookups, but the math-heavy scoring functions don't touch it, so a
 * mocked Context is enough.
 */
class AppCategoryProviderTest {

    private lateinit var provider: AppCategoryProvider

    @Before fun setUp() {
        provider = AppCategoryProvider(mockk(relaxed = true))
    }

    @Test fun `score is zero for an app the user has never used`() {
        val emptySnapshot = UsageSnapshot("com.example", 0, 0, 0, emptyMap())
        val score = provider.computeWeightScore(emptySnapshot, AppCategory.UTILITIES, isSystemApp = false)
        assertEquals(0f, score, 0.01f)
    }

    @Test fun `unused system app receives penalty and clamps to zero`() {
        val emptySnapshot = UsageSnapshot("com.android.calculator", 0, 0, 0, emptyMap())
        val score = provider.computeWeightScore(emptySnapshot, AppCategory.SYSTEM, isSystemApp = true)
        assertEquals("unused system app should be pushed to 0", 0f, score, 0.01f)
    }

    @Test fun `recently used app outranks long-ago used app of same category`() {
        val now = System.currentTimeMillis()
        val recent = UsageSnapshot("a", 60_000, now - 60_000, 5, mapOf(8 to 5))
        val old = UsageSnapshot("b", 60_000, now - (10 * 3600_000L), 5, mapOf(8 to 5))
        val recentScore = provider.computeWeightScore(recent, AppCategory.UTILITIES, false, now)
        val oldScore = provider.computeWeightScore(old, AppCategory.UTILITIES, false, now)
        assertTrue("$recentScore should be > $oldScore", recentScore > oldScore)
    }

    @Test fun `frequency increases score but does not saturate immediately`() {
        val now = System.currentTimeMillis()
        val sparse = UsageSnapshot("a", 60_000, now, 1, emptyMap())
        val active = UsageSnapshot("b", 60_000, now, 20, emptyMap())
        val sparseScore = provider.computeWeightScore(sparse, AppCategory.UTILITIES, false, now)
        val activeScore = provider.computeWeightScore(active, AppCategory.UTILITIES, false, now)
        assertTrue(activeScore > sparseScore)
        assertTrue("score must remain normalized to [0,1]", activeScore <= 1f)
    }

    @Test fun `rankApps sorts descending by score`() {
        val apps = listOf(
            AppInfo("a", "A", null, AppCategory.UTILITIES, false, 0, 0),
            AppInfo("b", "B", null, AppCategory.UTILITIES, false, 0, 0),
            AppInfo("c", "C", null, AppCategory.UTILITIES, false, 0, 0)
        )
        val now = System.currentTimeMillis()
        val snapshots = mapOf(
            "a" to UsageSnapshot("a", 30_000, now - 60_000, 5, mapOf(8 to 5)),
            "b" to UsageSnapshot("b", 30_000, now - (5 * 3600_000L), 1, emptyMap()),
            "c" to UsageSnapshot("c", 60_000, now - 5000, 20, mapOf(8 to 20))
        )
        val ranked = provider.rankApps(apps, snapshots)
        assertEquals(3, ranked.size)
        for (i in 0 until ranked.size - 1) {
            assertTrue(
                "score[$i]=${ranked[i].weightScore} must be >= score[${i+1}]=${ranked[i+1].weightScore}",
                ranked[i].weightScore >= ranked[i + 1].weightScore
            )
        }
    }

    @Test fun `autoGroup omits SYSTEM bucket and singletons`() {
        val now = System.currentTimeMillis()
        val ranked = listOf(
            rankedAppOf("a", AppCategory.SOCIAL, 0.8f),
            rankedAppOf("b", AppCategory.SOCIAL, 0.7f),
            rankedAppOf("c", AppCategory.WORK, 0.5f),
            rankedAppOf("d", AppCategory.SYSTEM, 0.9f),
            rankedAppOf("e", AppCategory.SYSTEM, 0.8f)
        )
        val groups = provider.autoGroup(ranked).toMap()
        assertTrue("SYSTEM category must be filtered out", AppCategory.SYSTEM !in groups)
        assertTrue("singleton WORK group should be filtered", AppCategory.WORK !in groups)
        assertEquals(2, groups[AppCategory.SOCIAL]?.size)
    }

    private fun rankedAppOf(pkg: String, cat: AppCategory, score: Float) =
        com.ailauncher.app.domain.models.RankedApp(
            app = AppInfo(pkg, pkg, null, cat, false, 0, 0),
            weightScore = score,
            usageMinutesToday = 1,
            launchCountToday = 1,
            lastUsedTimestamp = 0,
            smartFolder = cat
        )
}
