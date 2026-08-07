package com.ailauncher.app.data.ml

import android.content.Context
import com.ailauncher.app.data.db.HourlyUsageEntity
import com.ailauncher.app.data.db.PredictionCacheEntity
import com.ailauncher.app.data.db.UsageCacheEntity
import com.ailauncher.app.data.db.UsageDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * v7 FIX #1: Replaced runBlocking + AbortFlowException hack with proper Flow.first().
 *
 * On-device app prediction engine. Designed to be upgraded to TFLite without breaking the API:
 * - Feature extraction stays the same
 * - Only computeFeatureScore() body changes when TFLite model is added
 */
@Singleton
class AppPredictionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageDao: UsageDao
) {

    data class UsageFeature(
        val packageName: String,
        val recencyHours: Double,
        val frequencyLog: Float,
        val hourOfDayMatch: Float,
        val dayOfWeekMatch: Float,
        val avgSessionMin: Float,
        val lastLaunchesPerHour: Float
    )

    companion object {
        private const val W_RECENCY = 0.35f
        private const val W_FREQUENCY = 0.25f
        private const val W_HOUR_MATCH = 0.20f
        private const val W_DAY_MATCH = 0.10f
        private const val W_SESSION = 0.05f
        private const val W_RATE = 0.05f
        private const val RECENCY_DECAY_HOURS = 4.0
    }

    fun predictionsFlow(): Flow<Map<String, Float>> =
        usageDao.getAllPredictionsFlow().map { list ->
            list.associate { it.packageName to it.predictedScore }
        }

    suspend fun predict() = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val now = System.currentTimeMillis()

        val allUsage: List<UsageCacheEntity> = usageDao.getAllFlow().first()

        val predictions = allUsage.map { usage ->
            val hourlyData = usageDao.getHourly(usage.packageName)
            val feature = extractFeatures(usage, hourlyData, currentHour, currentDay, now)
            val score = computeFeatureScore(feature)
            PredictionCacheEntity(packageName = usage.packageName, predictedScore = score, lastPredicted = now)
        }

        predictions.forEach { usageDao.upsertPrediction(it) }
        usageDao.deleteOldPredictions(now - 24 * 60 * 60 * 1000L)
    }

    // internal (not private) so AppPredictionEngineTest can exercise the pure math
    // directly without booting Room/Hilt.
    internal fun extractFeatures(
        usage: UsageCacheEntity, hourlyData: List<HourlyUsageEntity>,
        currentHour: Int, currentDay: Int, now: Long
    ): UsageFeature {
        val recencyHours = (now - usage.lastTimeUsed).toDouble() / 3_600_000.0
        val freqLog = if (usage.launchCount > 0) ln(usage.launchCount.toFloat() + 1).coerceAtMost(5f) / 5f else 0f
        val totalHourly = hourlyData.sumOf { it.count }
        val nearbyHours = setOf((currentHour - 1 + 24) % 24, currentHour, (currentHour + 1) % 24)
        val nearbyCount = hourlyData.filter { it.hour in nearbyHours }.sumOf { it.count }
        val hourMatch = if (totalHourly > 0) (nearbyCount.toFloat() / totalHourly).coerceAtMost(1f) else 0f
        val isWeekend = currentDay == Calendar.SATURDAY || currentDay == Calendar.FRIDAY
        val dayMatch = if (isWeekend) 0.5f else 0.8f
        val avgSessionMin = if (usage.launchCount > 0)
            (usage.totalTimeInForeground / usage.launchCount / 60_000f).coerceAtMost(30f) / 30f else 0f
        val rate = (usage.launchCount.toFloat() / max(1f, recencyHours.toFloat())).coerceAtMost(10f) / 10f
        return UsageFeature(usage.packageName, recencyHours, freqLog, hourMatch, dayMatch, avgSessionMin, rate)
    }

    internal fun computeFeatureScore(f: UsageFeature): Float {
        val recencyScore = exp(-f.recencyHours / RECENCY_DECAY_HOURS).toFloat()
        return (W_RECENCY * recencyScore + W_FREQUENCY * f.frequencyLog +
                W_HOUR_MATCH * f.hourOfDayMatch + W_DAY_MATCH * f.dayOfWeekMatch +
                W_SESSION * f.avgSessionMin + W_RATE * f.lastLaunchesPerHour).coerceIn(0f, 1f)
    }
}
