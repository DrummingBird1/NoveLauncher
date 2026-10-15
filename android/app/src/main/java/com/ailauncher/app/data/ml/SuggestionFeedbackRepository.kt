package com.ailauncher.app.data.ml

import com.ailauncher.app.data.db.SuggestionFeedbackDao
import com.ailauncher.app.data.db.SuggestionFeedbackEntity
import com.ailauncher.app.domain.ranking.FeedbackScoring
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v10: records what "Recommended now" showed and what the user tapped there,
 * and hands GetRankedAppsUseCase the decayed counts. Stays on-device (Room),
 * excluded from Android auto-backup with the rest of launcher.db.
 */
@Singleton
class SuggestionFeedbackRepository @Inject constructor(
    private val dao: SuggestionFeedbackDao
) {
    private val mutex = Mutex()

    data class Counts(val taps: Float, val impressions: Float)

    suspend fun recordImpressions(packages: Collection<String>, now: Long = System.currentTimeMillis()) =
        update(packages, now) { it.copy(impressions = it.impressions + 1f) }

    suspend fun recordTap(packageName: String, now: Long = System.currentTimeMillis()) =
        update(listOf(packageName), now) { it.copy(taps = it.taps + 1f) }

    /** Decayed counts for every app with any feedback. */
    suspend fun counts(now: Long = System.currentTimeMillis()): Map<String, Counts> = try {
        dao.getAll().associate { e ->
            val elapsed = now - e.updatedAt
            e.packageName to Counts(FeedbackScoring.decay(e.taps, elapsed), FeedbackScoring.decay(e.impressions, elapsed))
        }
    } catch (e: Exception) {
        Timber.w(e, "Reading suggestion feedback failed")
        emptyMap()
    }

    suspend fun clear() = dao.clear()

    private suspend fun update(
        packages: Collection<String>,
        now: Long,
        change: (SuggestionFeedbackEntity) -> SuggestionFeedbackEntity
    ) {
        if (packages.isEmpty()) return
        try {
            mutex.withLock {
                val existing = dao.getFor(packages.toList()).associateBy { it.packageName }
                val updated = packages.distinct().map { pkg ->
                    val current = existing[pkg]?.let { e ->
                        val elapsed = now - e.updatedAt
                        e.copy(
                            taps = FeedbackScoring.decay(e.taps, elapsed),
                            impressions = FeedbackScoring.decay(e.impressions, elapsed),
                            updatedAt = now
                        )
                    } ?: SuggestionFeedbackEntity(pkg, impressions = 0f, taps = 0f, updatedAt = now)
                    change(current)
                }
                dao.upsertAll(updated)
            }
        } catch (e: Exception) {
            Timber.w(e, "Recording suggestion feedback failed")
        }
    }
}
