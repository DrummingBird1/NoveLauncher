package com.ailauncher.app.data.db

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies every migration in Migrations.kt against the real schema history
 * exported to app/schemas/ (room.schemaLocation, configured in
 * app/build.gradle.kts) — not a hand-maintained copy of the SQL. Catches both
 * "the index/table SQL doesn't match what Room expects" (runMigrationsAndValidate
 * throws on mismatch) and "the migration lost data."
 *
 * v10: uses Room 2.8's driver-based MigrationTestHelper (file + SQLiteDriver).
 * The legacy name-based constructor compares the configured database *name*
 * against the resolved *path* with a '/'-only split, which fails on Windows.
 *
 * `application = Application::class` — same reasoning as SettingsRepositoryTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class LauncherDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath("launcher-migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = LauncherDatabase::class
    )

    @Test
    fun `migrate 1 to 2 preserves existing rows and adds the expected indices`() {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO daily_stats (id, date, packageName, screenTimeMs, launches) " +
                    "VALUES (1, '2026-08-01', 'com.example.app', 60000, 3)"
            )
            execSQL("INSERT INTO hourly_usage (id, packageName, hour, count) VALUES (1, 'com.example.app', 9, 2)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

        migrated.prepare("SELECT screenTimeMs, launches FROM daily_stats WHERE packageName = 'com.example.app'").use { st ->
            assertTrue("pre-migration row should survive the migration", st.step())
            assertEquals(60000, st.getInt(0))
            assertEquals(3, st.getInt(1))
        }
        for (indexName in listOf("index_daily_stats_date", "index_daily_stats_packageName", "index_hourly_usage_packageName")) {
            assertTrue("expected $indexName to exist after migration", migrated.hasIndex(indexName))
        }
        migrated.close()
    }

    @Test
    fun `migrate 2 to 3 adds suggestion_feedback and keeps existing data`() {
        helper.createDatabase(2).apply {
            execSQL("INSERT INTO hourly_usage (id, packageName, hour, count) VALUES (1, 'com.example.app', 9, 2)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3))

        migrated.prepare("SELECT count FROM hourly_usage WHERE packageName = 'com.example.app'").use { st ->
            assertTrue(st.step())
            assertEquals(2, st.getInt(0))
        }
        migrated.execSQL(
            "INSERT INTO suggestion_feedback (packageName, impressions, taps, updatedAt) VALUES ('com.example.app', 3.0, 1.0, 0)"
        )
        migrated.prepare("SELECT taps FROM suggestion_feedback").use { st ->
            assertTrue(st.step())
            assertEquals(1.0, st.getDouble(0), 0.0001)
        }
        migrated.close()
    }

    @Test
    fun `a v1 database upgrades all the way to the current schema`() {
        helper.createDatabase(1).close()
        helper.runMigrationsAndValidate(3, listOf(MIGRATION_1_2, MIGRATION_2_3)).close()
    }

    private fun SQLiteConnection.hasIndex(name: String): Boolean =
        prepare("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?").use { st ->
            st.bindText(1, name)
            st.step()
        }
}
