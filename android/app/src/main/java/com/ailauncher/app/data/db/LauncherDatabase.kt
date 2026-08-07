package com.ailauncher.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entities ─────────────────────────────────────────────────────────

@Entity(tableName = "usage_cache")
data class UsageCacheEntity(
    @PrimaryKey val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val launchCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "hourly_usage")
data class HourlyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val hour: Int,
    val count: Int
)

@Entity(tableName = "notification_tracking")
data class NotificationEntity(
    @PrimaryKey val packageName: String,
    val unreadCount: Int = 0,
    val lastNotificationTime: Long = 0,
    val lastOpenedTime: Long = 0
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,             // yyyy-MM-dd
    val packageName: String,
    val screenTimeMs: Long,
    val launches: Int
)

@Entity(tableName = "prediction_cache")
data class PredictionCacheEntity(
    @PrimaryKey val packageName: String,
    val predictedScore: Float,
    val lastPredicted: Long = System.currentTimeMillis()
)

// ── DAOs ─────────────────────────────────────────────────────────────

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_cache") fun getAllFlow(): Flow<List<UsageCacheEntity>>
    @Query("SELECT * FROM usage_cache WHERE packageName = :pkg") suspend fun get(pkg: String): UsageCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: UsageCacheEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(entities: List<UsageCacheEntity>)
    @Query("DELETE FROM usage_cache WHERE lastUpdated < :cutoff") suspend fun deleteOld(cutoff: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHourly(entity: HourlyUsageEntity)
    @Query("SELECT * FROM hourly_usage WHERE packageName = :pkg") suspend fun getHourly(pkg: String): List<HourlyUsageEntity>
    @Query("DELETE FROM hourly_usage") suspend fun clearHourly()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDailyStats(entity: DailyStatsEntity)
    @Query("SELECT * FROM daily_stats WHERE date >= :fromDate ORDER BY date DESC") suspend fun getStatsFrom(fromDate: String): List<DailyStatsEntity>
    @Query("SELECT packageName, SUM(screenTimeMs) as screenTimeMs, SUM(launches) as launches, '' as date, 0 as id FROM daily_stats WHERE date >= :fromDate GROUP BY packageName ORDER BY screenTimeMs DESC LIMIT :limit")
    suspend fun getTopAppsByScreenTime(fromDate: String, limit: Int = 10): List<DailyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPrediction(entity: PredictionCacheEntity)
    @Query("SELECT * FROM prediction_cache") fun getAllPredictionsFlow(): Flow<List<PredictionCacheEntity>>
    @Query("DELETE FROM prediction_cache WHERE lastPredicted < :cutoff") suspend fun deleteOldPredictions(cutoff: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_tracking") fun getAllFlow(): Flow<List<NotificationEntity>>
    @Query("SELECT * FROM notification_tracking WHERE packageName = :pkg") suspend fun get(pkg: String): NotificationEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: NotificationEntity)

    @Query("UPDATE notification_tracking SET unreadCount = unreadCount + 1, lastNotificationTime = :time WHERE packageName = :pkg")
    suspend fun incrementUnread(pkg: String, time: Long)

    @Query("UPDATE notification_tracking SET unreadCount = 0, lastOpenedTime = :time WHERE packageName = :pkg")
    suspend fun markOpened(pkg: String, time: Long)

    @Query("SELECT packageName FROM notification_tracking WHERE unreadCount > 0") suspend fun getAppsWithUnread(): List<String>
}

// ── Database ─────────────────────────────────────────────────────────

@Database(
    entities = [
        UsageCacheEntity::class,
        HourlyUsageEntity::class,
        NotificationEntity::class,
        DailyStatsEntity::class,
        PredictionCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun notificationDao(): NotificationDao
}
