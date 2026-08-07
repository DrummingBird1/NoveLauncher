package com.ailauncher.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.RankedApp
import com.ailauncher.app.domain.models.UsageSnapshot
import java.util.Calendar
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * On-device AI/heuristic engine for app classification and ranking.
 *
 * Battery-efficient design:
 * - Pure CPU computation, no network calls
 * - Results cached and recomputed only on trigger (new app, time-window change)
 * - All math uses simple floating-point ops (no ML runtime overhead)
 */
class AppCategoryProvider(private val context: Context) {

    // ── Scoring weights ──────────────────────────────────────────────
    companion object {
        private const val WEIGHT_RECENCY = 0.35f
        private const val WEIGHT_FREQUENCY = 0.30f
        private const val WEIGHT_TIME_OF_DAY = 0.25f
        private const val WEIGHT_CATEGORY_BOOST = 0.10f

        // Recency decay: after 4 hours the recency score drops to ~0.1
        private const val RECENCY_DECAY_HOURS = 4.0

        // Known package → category overrides (reliable fallback)
        private val PACKAGE_OVERRIDES = mapOf(
            "com.whatsapp" to AppCategory.SOCIAL,
            "com.instagram.android" to AppCategory.SOCIAL,
            "com.facebook.katana" to AppCategory.SOCIAL,
            "com.twitter.android" to AppCategory.SOCIAL,
            "com.tencent.mm" to AppCategory.SOCIAL,
            "com.google.android.youtube" to AppCategory.ENTERTAINMENT,
            "com.spotify.music" to AppCategory.ENTERTAINMENT,
            "com.netflix.mediaclient" to AppCategory.ENTERTAINMENT,
            "com.google.android.apps.maps" to AppCategory.UTILITIES,
            "com.waze" to AppCategory.UTILITIES,
            "com.google.android.gm" to AppCategory.WORK,
            "com.microsoft.office.outlook" to AppCategory.WORK,
            "com.slack" to AppCategory.WORK,
            "com.google.android.apps.docs" to AppCategory.WORK,
            "com.amazon.mShop.android.shopping" to AppCategory.SHOPPING,
            "com.aliexpress" to AppCategory.SHOPPING,
            "com.zhiliaoapp.musically" to AppCategory.ENTERTAINMENT, // TikTok
        )

        // Time-of-day category affinity matrix
        // Each hour maps to categories that get a boost at that time
        private val TIME_CATEGORY_AFFINITY: Map<IntRange, Set<AppCategory>> = mapOf(
            (6..9) to setOf(AppCategory.UTILITIES, AppCategory.HEALTH),           // morning
            (9..17) to setOf(AppCategory.WORK, AppCategory.EDUCATION),            // work hours
            (17..21) to setOf(AppCategory.SOCIAL, AppCategory.ENTERTAINMENT),     // evening
            (21..23) to setOf(AppCategory.ENTERTAINMENT, AppCategory.GAMES),      // night
            (0..5) to setOf(AppCategory.ENTERTAINMENT, AppCategory.SOCIAL),       // late night
        )
    }

    // ── Classification ───────────────────────────────────────────────

    /**
     * Categorize a single app using a 3-tier fallback:
     * 1. Known package override table
     * 2. Play Store category from ApplicationInfo (Android 8+)
     * 3. Heuristic: system flag → SYSTEM, else UNCATEGORIZED
     */
    fun categorize(packageName: String, applicationInfo: ApplicationInfo?): AppCategory {
        // Tier 1: hardcoded overrides for popular apps
        PACKAGE_OVERRIDES[packageName]?.let { return it }

        // Tier 2: Play Store category metadata
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && applicationInfo != null) {
            val playCat = applicationInfo.category
            val mapped = mapAndroidCategoryToEnum(playCat)
            if (mapped != AppCategory.UNCATEGORIZED) return mapped
        }

        // Tier 3: system app heuristic
        if (applicationInfo != null &&
            (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        ) {
            return AppCategory.SYSTEM
        }

        return AppCategory.UNCATEGORIZED
    }

    /**
     * Batch-categorize all launchable apps.
     */
    fun categorizeAll(apps: List<Pair<String, ApplicationInfo?>>): Map<String, AppCategory> {
        return apps.associate { (pkg, info) -> pkg to categorize(pkg, info) }
    }

    // ── Scoring ──────────────────────────────────────────────────────

    /**
     * Compute importance score for a single app at the current moment.
     * Returns 0.0..1.0 normalized score.
     *
     * v8: System apps with no actual usage are penalised so they don't crowd the
     * "recommended now" rail. Once a system app accumulates real launches the
     * frequency/recency signals win out anyway.
     */
    fun computeWeightScore(
        snapshot: UsageSnapshot,
        category: AppCategory,
        isSystemApp: Boolean = false,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Float {
        val recency = computeRecencyScore(snapshot.lastTimeUsed, currentTimeMillis)
        val frequency = computeFrequencyScore(snapshot.launchCount, snapshot.totalTimeInForeground)
        val timeOfDay = computeTimeOfDayScore(snapshot.hourlyDistribution, category)
        val categoryBoost = computeCategoryBoost(category)

        val raw = (WEIGHT_RECENCY * recency) +
                  (WEIGHT_FREQUENCY * frequency) +
                  (WEIGHT_TIME_OF_DAY * timeOfDay) +
                  (WEIGHT_CATEGORY_BOOST * categoryBoost)

        // System apps that the user has never launched (no recency signal) get pushed down.
        // Anything the user has actually used keeps its score.
        val systemPenalty = if (isSystemApp && snapshot.launchCount == 0) 0.5f else 0f
        return (raw - systemPenalty).coerceIn(0f, 1f)
    }

    /**
     * Rank a full list of apps. Returns sorted descending by score.
     * This is the main entry point called by the UseCase layer.
     */
    fun rankApps(
        apps: List<AppInfo>,
        snapshots: Map<String, UsageSnapshot>
    ): List<RankedApp> {
        val now = System.currentTimeMillis()
        return apps.map { app ->
            val snapshot = snapshots[app.packageName] ?: UsageSnapshot(
                packageName = app.packageName,
                totalTimeInForeground = 0,
                lastTimeUsed = 0,
                launchCount = 0,
                hourlyDistribution = emptyMap()
            )
            val score = computeWeightScore(snapshot, app.category, app.isSystemApp, now)
            RankedApp(
                app = app,
                weightScore = score,
                usageMinutesToday = snapshot.totalTimeInForeground / 60_000,
                launchCountToday = snapshot.launchCount,
                lastUsedTimestamp = snapshot.lastTimeUsed,
                smartFolder = app.category
            )
        }.sortedByDescending { it.weightScore }
    }

    // ── Auto-grouping (Smart Folders) ────────────────────────────────

    /**
     * Group ranked apps into smart folders.
     * Filters out folders with fewer than 2 apps.
     */
    fun autoGroup(rankedApps: List<RankedApp>): List<Pair<AppCategory, List<RankedApp>>> {
        return rankedApps
            .groupBy { it.smartFolder }
            .filter { (cat, apps) -> cat != AppCategory.SYSTEM && apps.size >= 2 }
            .toList()
            .sortedByDescending { (_, apps) -> apps.maxOf { it.weightScore } }
    }

    // ── Private scoring functions ────────────────────────────────────

    /**
     * Exponential decay: score drops as time since last use increases.
     * At t=0 → 1.0, at t=RECENCY_DECAY_HOURS → ~0.37, at t=2×decay → ~0.14
     */
    private fun computeRecencyScore(lastUsed: Long, now: Long): Float {
        if (lastUsed == 0L) return 0f
        val hoursSinceUse = (now - lastUsed).toDouble() / 3_600_000.0
        return exp(-hoursSinceUse / RECENCY_DECAY_HOURS).toFloat()
    }

    /**
     * Frequency score: log-scaled launch count + time weight.
     * Prevents heavy apps from completely dominating.
     */
    private fun computeFrequencyScore(launchCount: Int, totalForegroundMs: Long): Float {
        if (launchCount == 0) return 0f
        val launchScore = min(1.0f, kotlin.math.ln(launchCount.toFloat() + 1) / 4.0f)
        val timeScore = min(1.0f, (totalForegroundMs / 3_600_000f)) // cap at 1hr
        return (launchScore * 0.6f + timeScore * 0.4f)
    }

    /**
     * Does the user historically use this app at the current hour?
     * Also applies category-time affinity boost.
     */
    private fun computeTimeOfDayScore(
        hourlyDist: Map<Int, Int>,
        category: AppCategory
    ): Float {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Personal pattern score: did the user use THIS app at this hour?
        val totalLaunches = hourlyDist.values.sum().toFloat()
        val patternScore = if (totalLaunches > 0) {
            val nearbyHours = listOf(
                (currentHour - 1 + 24) % 24,
                currentHour,
                (currentHour + 1) % 24
            )
            val nearbyLaunches = nearbyHours.sumOf { hourlyDist[it] ?: 0 }
            min(1f, nearbyLaunches / max(1f, totalLaunches) * 3f)
        } else 0f

        // Category affinity boost for current time window
        val affinityScore = TIME_CATEGORY_AFFINITY.entries
            .firstOrNull { (range, _) -> currentHour in range }
            ?.let { (_, cats) -> if (category in cats) 0.3f else 0f }
            ?: 0f

        return min(1f, patternScore + affinityScore)
    }

    /**
     * Small static boost for contextually relevant categories.
     * Work apps get boosted on weekdays; entertainment on weekends.
     */
    private fun computeCategoryBoost(category: AppCategory): Float {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.FRIDAY // Israel weekend

        return when {
            !isWeekend && category == AppCategory.WORK -> 0.8f
            isWeekend && category in setOf(AppCategory.ENTERTAINMENT, AppCategory.GAMES) -> 0.7f
            category == AppCategory.SOCIAL -> 0.5f // always somewhat relevant
            category == AppCategory.SYSTEM -> 0.1f // deprioritize
            else -> 0.3f
        }
    }

    /**
     * Map Android ApplicationInfo.category int to our enum.
     */
    private fun mapAndroidCategoryToEnum(androidCategory: Int): AppCategory {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return AppCategory.UNCATEGORIZED
        return when (androidCategory) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.PHOTOGRAPHY
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.UTILITIES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.SYSTEM
            else -> AppCategory.UNCATEGORIZED
        }
    }
}
