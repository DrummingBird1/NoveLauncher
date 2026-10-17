package com.ailauncher.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ailauncher.app.domain.models.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Efficient wrapper around UsageStatsManager.
 *
 * Battery strategy:
 * - Queries only the last 24 hours by default (minimizes I/O)
 * - Caches results; caller controls refresh interval via ViewModel
 * - Uses UsageEvents API for accurate launch counts (not queryUsageStats)
 */
class UsageStatsRepository(
    private val context: Context,
) {
    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    /**
     * Check if the user has granted PACKAGE_USAGE_STATS permission.
     */
    fun hasPermission(): Boolean {
        val manager = usageStatsManager ?: return false
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val stats =
            manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                System.currentTimeMillis(),
            )
        return stats?.isNotEmpty() == true
    }

    /**
     * Collect usage snapshots for the last 24 hours.
     * Runs on Dispatchers.Default to avoid blocking UI.
     *
     * v9: a single Calendar instance is reused for hour extraction. A typical
     * 24h window on a phone has ~1–2k UsageEvents, and Calendar.getInstance()
     * touches the default TimeZone and Locale registry — measurable GC churn
     * during the periodic refresh on low-end devices.
     */
    suspend fun getUsageSnapshots(): Map<String, UsageSnapshot> =
        withContext(Dispatchers.Default) {
            val manager = usageStatsManager ?: return@withContext emptyMap()
            val now = System.currentTimeMillis()
            val startTime = now - 24 * 60 * 60 * 1000L // 24 hours ago

            // Use events API for precise per-launch tracking
            val events = manager.queryEvents(startTime, now)
            val event = UsageEvents.Event()
            val hourCal = Calendar.getInstance() // v9: reuse

            // Accumulators per package
            val launchCounts = mutableMapOf<String, Int>()
            val lastUsedTimes = mutableMapOf<String, Long>()
            val foregroundTimes = mutableMapOf<String, Long>()
            val hourlyDist = mutableMapOf<String, MutableMap<Int, Int>>()
            val moveToForegroundTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    1, // MOVE_TO_FOREGROUND fallback
                    -> {
                        moveToForegroundTimes[pkg] = event.timeStamp
                        launchCounts[pkg] = (launchCounts[pkg] ?: 0) + 1
                        lastUsedTimes[pkg] = maxOf(lastUsedTimes[pkg] ?: 0L, event.timeStamp)

                        // Track hourly distribution — reuse Calendar instance.
                        hourCal.timeInMillis = event.timeStamp
                        val hour = hourCal.get(Calendar.HOUR_OF_DAY)
                        val pkgHourly = hourlyDist.getOrPut(pkg) { mutableMapOf() }
                        pkgHourly[hour] = (pkgHourly[hour] ?: 0) + 1
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    2, // MOVE_TO_BACKGROUND fallback
                    -> {
                        val fgTime = moveToForegroundTimes.remove(pkg) ?: continue
                        val duration = event.timeStamp - fgTime
                        if (duration in 1..3_600_000) { // sanity: 0–1hr per session
                            foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0L) + duration
                        }
                    }
                }
            }

            // Build snapshots
            val allPackages = launchCounts.keys + lastUsedTimes.keys
            allPackages.associateWith { pkg ->
                UsageSnapshot(
                    packageName = pkg,
                    totalTimeInForeground = foregroundTimes[pkg] ?: 0L,
                    lastTimeUsed = lastUsedTimes[pkg] ?: 0L,
                    launchCount = launchCounts[pkg] ?: 0,
                    hourlyDistribution = hourlyDist[pkg]?.toMap() ?: emptyMap(),
                )
            }
        }

    /** v10: one app's totals over a date range, for the Statistics page. */
    data class AppUsageTotal(
        val packageName: String,
        val screenTimeMs: Long,
        val launches: Int,
    )

    /**
     * v10: per-app screen time and launches between [fromMillis] and now.
     *
     * The Statistics page used to read a Room table (daily_stats) that nothing
     * ever wrote to, so it was always empty. This asks UsageStatsManager
     * directly: screen time from the aggregated stats (kept for weeks to months
     * at daily/weekly granularity), launches from the event log (kept for
     * roughly the last week or so, so month-range launch counts are a floor).
     */
    suspend fun getRangeTotals(
        fromMillis: Long,
        excludePackage: String? = null,
    ): List<AppUsageTotal> =
        withContext(Dispatchers.Default) {
            val manager = usageStatsManager ?: return@withContext emptyList()
            val now = System.currentTimeMillis()
            val aggregated =
                try {
                    manager.queryAndAggregateUsageStats(fromMillis, now)
                } catch (_: Exception) {
                    emptyMap()
                }

            val launches = mutableMapOf<String, Int>()
            try {
                val events = manager.queryEvents(fromMillis, now)
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        val pkg = event.packageName ?: continue
                        launches[pkg] = (launches[pkg] ?: 0) + 1
                    }
                }
            } catch (_: Exception) {
            }

            (aggregated.keys + launches.keys)
                .asSequence()
                .filter { it != excludePackage }
                .map { pkg -> AppUsageTotal(pkg, aggregated[pkg]?.totalTimeInForeground ?: 0L, launches[pkg] ?: 0) }
                .filter { it.screenTimeMs >= 60_000 || it.launches > 0 }
                .sortedByDescending { it.screenTimeMs }
                .toList()
        }
}
