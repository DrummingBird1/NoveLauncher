package com.ailauncher.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * v10: a migration expressed as plain SQL statements, runnable through either of
 * Room's two database APIs. The app opens LauncherDatabase through the classic
 * SupportSQLiteOpenHelper path (→ migrate(SupportSQLiteDatabase)), while Room
 * 2.8's driver-based MigrationTestHelper used by LauncherDatabaseMigrationTest
 * hands over a SQLiteConnection (→ migrate(SQLiteConnection)). Overriding only
 * one of them makes the other path throw NotImplementedError.
 */
private class SqlMigration(from: Int, to: Int, private val statements: List<String>) : Migration(from, to) {
    override fun migrate(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)
    override fun migrate(connection: SQLiteConnection) = statements.forEach { connection.execSQL(it) }
}

/**
 * v9.3: adds indices on daily_stats(date), daily_stats(packageName), and
 * hourly_usage(packageName) — see the kdocs on those entities in
 * LauncherDatabase.kt for which queries they speed up. Additive-only (no
 * column/table changes), so this is a real migration rather than reaching for
 * fallbackToDestructiveMigration() — existing usage/stats caches survive the
 * upgrade instead of being wiped.
 *
 * Names must match Room's own `index_<table>_<column>` convention exactly —
 * that's what Room's schema validator compares the live SQLite schema against
 * on next open, and a mismatch throws IllegalStateException at runtime.
 */
val MIGRATION_1_2: Migration = SqlMigration(
    1, 2,
    listOf(
        "CREATE INDEX IF NOT EXISTS `index_daily_stats_date` ON `daily_stats` (`date`)",
        "CREATE INDEX IF NOT EXISTS `index_daily_stats_packageName` ON `daily_stats` (`packageName`)",
        "CREATE INDEX IF NOT EXISTS `index_hourly_usage_packageName` ON `hourly_usage` (`packageName`)"
    )
)

/**
 * v10: suggestion_feedback — impressions vs. taps per app in "Recommended now",
 * used to learn which suggestions the user actually wants (see FeedbackScoring).
 * Purely additive. The CREATE statement must match Room's generated schema
 * exactly (schemas/…/3.json); LauncherDatabaseMigrationTest validates it.
 */
val MIGRATION_2_3: Migration = SqlMigration(
    2, 3,
    listOf(
        "CREATE TABLE IF NOT EXISTS `suggestion_feedback` (`packageName` TEXT NOT NULL, " +
            "`impressions` REAL NOT NULL, `taps` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`packageName`))"
    )
)
