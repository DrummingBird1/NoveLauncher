package com.ailauncher.app.data.db

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies MIGRATION_1_2 (see Migrations.kt) against the real schema history
 * exported to app/schemas/ (room.schemaLocation, configured in
 * app/build.gradle.kts) — not a hand-maintained copy of the SQL. Catches both
 * "the index name/SQL doesn't match what Room expects" (validate=true makes
 * runMigrationsAndValidate throw on mismatch) and "the migration lost data."
 *
 * `application = Application::class` — same reasoning as SettingsRepositoryTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class LauncherDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LauncherDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun `migrate 1 to 2 preserves existing rows and adds the expected indices`() {
        val dbName = "launcher-migration-test.db"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO daily_stats (id, date, packageName, screenTimeMs, launches) " +
                    "VALUES (1, '2026-08-01', 'com.example.app', 60000, 3)"
            )
            execSQL(
                "INSERT INTO hourly_usage (id, packageName, hour, count) VALUES (1, 'com.example.app', 9, 2)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        migrated.query("SELECT screenTimeMs, launches FROM daily_stats WHERE packageName = 'com.example.app'").use { cursor ->
            assertTrue("pre-migration row should survive the migration", cursor.moveToFirst())
            assertEquals(60000, cursor.getInt(0))
            assertEquals(3, cursor.getInt(1))
        }

        for (indexName in listOf("index_daily_stats_date", "index_daily_stats_packageName", "index_hourly_usage_packageName")) {
            migrated.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(indexName)).use { cursor ->
                assertTrue("expected $indexName to exist after migration", cursor.moveToFirst())
            }
        }
    }
}
