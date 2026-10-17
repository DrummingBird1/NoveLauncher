package com.ailauncher.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ── Entities ─────────────────────────────────────────────────────────

@Entity(tableName = "usage_cache")
data class UsageCacheEntity(
    @PrimaryKey val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val launchCount: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
)

// v9.3: indexed on packageName, the only column getHourly() filters by.
@Entity(tableName = "hourly_usage", indices = [Index("packageName")])
data class HourlyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val hour: Int,
    val count: Int,
)

@Entity(tableName = "notification_tracking")
data class NotificationEntity(
    @PrimaryKey val packageName: String,
    val unreadCount: Int = 0,
    val lastNotificationTime: Long = 0,
    val lastOpenedTime: Long = 0,
)

// v9.3: indexed on the two columns StatisticsScreen's queries filter/group by
// (getStatsFrom filters on date; getTopAppsByScreenTime filters on date and
// groups by packageName) — see Migrations.kt MIGRATION_1_2 for how existing
// installs pick these up without losing data.
@Entity(tableName = "daily_stats", indices = [Index("date"), Index("packageName")])
data class DailyStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val packageName: String,
    val screenTimeMs: Long,
    val launches: Int,
)

@Entity(tableName = "prediction_cache")
data class PredictionCacheEntity(
    @PrimaryKey val packageName: String,
    val predictedScore: Float,
    val lastPredicted: Long = System.currentTimeMillis(),
)

/**
 * v10: how often each app was *shown* in "Recommended now" versus actually
 * *tapped* there — the signal that lets the ranking learn from what the user
 * ignores, not just from what they open. Counts are Floats because they decay
 * exponentially (see FeedbackScoring), so old habits fade instead of sticking.
 */
@Entity(tableName = "suggestion_feedback")
data class SuggestionFeedbackEntity(
    @PrimaryKey val packageName: String,
    val impressions: Float,
    val taps: Float,
    val updatedAt: Long,
)

// ── DAOs ─────────────────────────────────────────────────────────────

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_cache")
    fun getAllFlow(): Flow<List<UsageCacheEntity>>

    @Query("SELECT * FROM usage_cache WHERE packageName = :pkg")
    suspend fun get(pkg: String): UsageCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UsageCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<UsageCacheEntity>)

    @Query("DELETE FROM usage_cache WHERE lastUpdated < :cutoff")
    suspend fun deleteOld(cutoff: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHourly(entity: HourlyUsageEntity)

    @Query("SELECT * FROM hourly_usage WHERE packageName = :pkg")
    suspend fun getHourly(pkg: String): List<HourlyUsageEntity>

    @Query("DELETE FROM hourly_usage")
    suspend fun clearHourly()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStats(entity: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date >= :fromDate ORDER BY date DESC")
    suspend fun getStatsFrom(fromDate: String): List<DailyStatsEntity>

    @Query(
        "SELECT packageName, SUM(screenTimeMs) as screenTimeMs, SUM(launches) as launches, '' as date, 0 as id FROM daily_stats WHERE date >= :fromDate GROUP BY packageName ORDER BY screenTimeMs DESC LIMIT :limit",
    )
    suspend fun getTopAppsByScreenTime(
        fromDate: String,
        limit: Int = 10,
    ): List<DailyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrediction(entity: PredictionCacheEntity)

    @Query("SELECT * FROM prediction_cache")
    fun getAllPredictionsFlow(): Flow<List<PredictionCacheEntity>>

    @Query("DELETE FROM prediction_cache WHERE lastPredicted < :cutoff")
    suspend fun deleteOldPredictions(cutoff: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_tracking")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notification_tracking WHERE packageName = :pkg")
    suspend fun get(pkg: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationEntity)

    @Query("UPDATE notification_tracking SET unreadCount = unreadCount + 1, lastNotificationTime = :time WHERE packageName = :pkg")
    suspend fun incrementUnread(
        pkg: String,
        time: Long,
    )

    @Query("UPDATE notification_tracking SET unreadCount = 0, lastOpenedTime = :time WHERE packageName = :pkg")
    suspend fun markOpened(
        pkg: String,
        time: Long,
    )

    @Query("SELECT packageName FROM notification_tracking WHERE unreadCount > 0")
    suspend fun getAppsWithUnread(): List<String>
}

@Dao
interface SuggestionFeedbackDao {
    @Query("SELECT * FROM suggestion_feedback")
    suspend fun getAll(): List<SuggestionFeedbackEntity>

    @Query("SELECT * FROM suggestion_feedback WHERE packageName IN (:packages)")
    suspend fun getFor(packages: List<String>): List<SuggestionFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SuggestionFeedbackEntity>)

    @Query("DELETE FROM suggestion_feedback")
    suspend fun clear()
}

// ── Database ─────────────────────────────────────────────────────────

@Database(
    entities = [
        UsageCacheEntity::class,
        HourlyUsageEntity::class,
        NotificationEntity::class,
        DailyStatsEntity::class,
        PredictionCacheEntity::class,
        SuggestionFeedbackEntity::class,
    ],
    // v9.3: 2 — added indices (MIGRATION_1_2). v10: 3 — suggestion_feedback (MIGRATION_2_3).
    version = 3,
    // v9.3: was false. Needed for LauncherDatabaseMigrationTest's
    // MigrationTestHelper, which verifies MIGRATION_1_2 against the real
    // generated schema rather than a hand-maintained copy of it.
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    abstract fun notificationDao(): NotificationDao

    abstract fun suggestionFeedbackDao(): SuggestionFeedbackDao
}
