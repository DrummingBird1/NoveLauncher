package com.ailauncher.app.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BackupSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * v9: exercises SettingsRepository's import/export + atomicity logic against a real
 * (temp-file) DataStore via Robolectric. SettingsRepository was made DataStore-
 * injectable for exactly this — each test gets its own store so there's no cross-test
 * bleed or "multiple DataStores for one file" error.
 *
 * Scope note: AndroidKeyStore is unavailable in the Robolectric JVM, so SecureCrypto's
 * encrypt() falls back to plain JSON here. These tests therefore cover the
 * import/export/blanking *logic* — they do NOT verify the AES-GCM encryption itself,
 * which requires an on-device (instrumented) test. The fallback is deliberate and is
 * what makes the round-trip assertions valid regardless of keystore availability.
 *
 * `application = Application::class` stops Robolectric from booting the real
 * @HiltAndroidApp AILauncherApp (whose onCreate would NPE without the Hilt graph).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SettingsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var dsScope: CoroutineScope
    private lateinit var repo: SettingsRepository

    @Before fun setUp() {
        dsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = dsScope) {
            File(tmp.root, "settings_${System.nanoTime()}.preferences_pb")
        }
        repo = SettingsRepository(ApplicationProvider.getApplicationContext(), ds)
    }

    @After fun tearDown() { dsScope.cancel() }

    @Test fun `export then import round-trips non-secret settings`() = runBlocking {
        repo.saveAppearance(AppearanceSettings(iconSizeDp = 72, gridColumns = 5))
        repo.saveBackup(BackupSettings(nasAddress = "https://nas.local", nasUsername = "u"))
        val json = repo.exportAllSettings()

        repo.resetAll()
        assertEquals("reset should restore the default icon size", 56, repo.appearanceFlow.first().iconSizeDp)

        assertTrue(repo.importAllSettings(json))
        assertEquals(72, repo.appearanceFlow.first().iconSizeDp)
        assertEquals(5, repo.appearanceFlow.first().gridColumns)
        assertEquals("https://nas.local", repo.backupFlow.first().nasAddress)
    }

    @Test fun `export never contains the NAS password`() = runBlocking {
        repo.saveBackup(
            BackupSettings(nasAddress = "https://nas.local", nasUsername = "u", nasPassword = "hunter2")
        )
        val json = repo.exportAllSettings()
        assertFalse("exportAllSettings must blank nasPassword", json.contains("hunter2"))
    }

    @Test fun `malformed import returns false and leaves state intact`() = runBlocking {
        repo.saveAppearance(AppearanceSettings(iconSizeDp = 64))
        assertFalse(repo.importAllSettings("{ not valid json"))
        assertEquals(64, repo.appearanceFlow.first().iconSizeDp)
    }

    @Test fun `saved backup round-trips its password in the live store`() = runBlocking {
        // Within the live DataStore (not the portable export) the password must
        // survive a save/load cycle so the NAS backup can actually authenticate.
        repo.saveBackup(BackupSettings(nasAddress = "https://nas.local", nasPassword = "hunter2"))
        assertEquals("hunter2", repo.backupFlow.first().nasPassword)
    }
}
