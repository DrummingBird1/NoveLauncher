package com.ailauncher.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_stats_date` ON `daily_stats` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_stats_packageName` ON `daily_stats` (`packageName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_hourly_usage_packageName` ON `hourly_usage` (`packageName`)")
    }
}
