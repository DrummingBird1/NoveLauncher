package com.ailauncher.app.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BackupSettings
import com.ailauncher.app.domain.models.ClockStyle
import com.ailauncher.app.domain.models.LauncherFont
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
    private lateinit var ds: DataStore<Preferences>
    private lateinit var repo: SettingsRepository

    @Before fun setUp() {
        dsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ds =
            PreferenceDataStoreFactory.create(scope = dsScope) {
                File(tmp.root, "settings_${System.nanoTime()}.preferences_pb")
            }
        repo = SettingsRepository(ApplicationProvider.getApplicationContext(), ds)
    }

    @After fun tearDown() {
        dsScope.cancel()
    }

    @Test fun `export then import round-trips non-secret settings`() =
        runBlocking {
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

    @Test fun `export never contains the NAS password`() =
        runBlocking {
            repo.saveBackup(
                BackupSettings(nasAddress = "https://nas.local", nasUsername = "u", nasPassword = "hunter2"),
            )
            val json = repo.exportAllSettings()
            assertFalse("exportAllSettings must blank nasPassword", json.contains("hunter2"))
        }

    @Test fun `malformed import returns false and leaves state intact`() =
        runBlocking {
            repo.saveAppearance(AppearanceSettings(iconSizeDp = 64))
            assertFalse(repo.importAllSettings("{ not valid json"))
            assertEquals(64, repo.appearanceFlow.first().iconSizeDp)
        }

    @Test fun `saved backup round-trips its password in the live store`() =
        runBlocking {
            // Within the live DataStore (not the portable export) the password must
            // survive a save/load cycle so the NAS backup can actually authenticate.
            repo.saveBackup(BackupSettings(nasAddress = "https://nas.local", nasPassword = "hunter2"))
            assertEquals("hunter2", repo.backupFlow.first().nasPassword)
        }

    // Settings fields deleted after 10.0.0 (they had no UI and no effect) are still in
    // every stored group and every backup written by 10.0.0 and earlier. The Json has
    // ignoreUnknownKeys, so those groups must still decode — a decode failure would
    // silently reset the whole group to its defaults. Each group below also carries a
    // non-default value that survived, so a fallback to defaults fails the test.
    private val oldAppearance =
        """
        {"iconSizeDp":64,"appFont":"HEEBO","folderFont":"RUBIK","uiFont":"ASSISTANT",
         "clock":{"fontSize":72,"showSearch":false,"showReminders":true,"clockStyle":"ANALOG","analogSize":200},
         "wallpaper":{"mode":"SINGLE","entries":[],"dimOnScroll":true,"lockScreenSameAsHome":false,
                      "lockScreenWallpaper":{"id":"w1","uri":"content://w1","brightness":1.0}},
         "lockScreen":{"customEnabled":true,"showClock":true,"alwaysOnDisplay":false,"aodBrightness":0.3,
                       "clockFont":"RUBIK","clockColor":"#FFFFFF"},
         "searchBar":{"visible":true,"heightDp":48,"cornerRadius":24,"showMicButton":true,
                      "showCameraButton":false,"backgroundColor":"","textColor":""},
         "sortAppsByCategory":false,
         "categoryTabs":{"enabled":true,"showBottomTabs":true,"categoryIconSize":24,"categoryFontSize":11,
                         "customCategories":[{"id":"c1","name":"Mine","iconName":"star","packages":["a.b"]}]}}
        """.trimIndent()
    private val oldPages =
        """
        {"pageLayouts":{"home":{"pageId":"home","showTopBar":false,"showBottomDock":true,
                                "dockApps":["com.a","com.b"],"gridColumns":5}}}
        """.trimIndent()
    private val oldSmartControl =
        """
        {"voiceControlEnabled":true,"gesturesEnabled":true,"doubleTapToSleep":false,"pinchToOverview":true,
         "shakeToSearch":true,"smartScreenOff":true,"hapticFeedback":false,"longPressAction":"hide",
         "homeButtonDoubleTap":"search","reduceMotion":true}
        """.trimIndent()
    private val oldAdaptive = """{"autoDetect":true,"tabletColumns":7,"scaleUi":1.2}"""
    private val oldHidden = """{"hiddenPackages":["h.x"],"privateFolderPackages":["p.x"],"privateFolderLocked":false}"""
    private val oldOnboarding = """{"completed":true,"currentStep":3,"lastSeenVersionCode":13}"""
    private val oldSecurity = """{"lockedAppPackages":["l.x"],"personalZoneLockMethod":"PIN","personalZonePin":"abc"}"""

    private suspend fun assertOldValuesSurvived() {
        val a = repo.appearanceFlow.first()
        assertEquals(64, a.iconSizeDp)
        assertEquals(LauncherFont.HEEBO, a.appFont)
        assertEquals(LauncherFont.RUBIK, a.folderFont)
        assertEquals(LauncherFont.ASSISTANT, a.uiFont)
        assertEquals(72, a.clock.fontSize)
        assertEquals(ClockStyle.ANALOG, a.clock.clockStyle)
        assertEquals(200, a.clock.analogSize)
        assertTrue(a.wallpaper.dimOnScroll)
        assertEquals(
            listOf("com.a", "com.b"),
            repo.pagesFlow
                .first()
                .pageLayouts["home"]
                ?.dockApps,
        )
        val smart = repo.smartControlFlow.first()
        assertFalse(smart.hapticFeedback)
        assertTrue(smart.reduceMotion)
        assertEquals(7, repo.adaptiveDisplayFlow.first().tabletColumns)
        assertEquals(setOf("p.x"), repo.hiddenAppsFlow.first().privateFolderPackages)
        assertEquals(13, repo.onboardingFlow.first().lastSeenVersionCode)
        val sec = repo.securityFlow.first()
        assertEquals(setOf("l.x"), sec.lockedAppPackages)
        assertEquals("abc", sec.personalZonePin)
    }

    @Test fun `stored settings that still carry removed fields decode`() =
        runBlocking {
            // Raw JSON written straight into the store, the way 10.0.0 left it. Security
            // goes in as plain JSON, which the legacy path still reads (no AndroidKeyStore here).
            ds.edit {
                it[stringPreferencesKey("appearance")] = oldAppearance
                it[stringPreferencesKey("pages")] = oldPages
                it[stringPreferencesKey("smart_control")] = oldSmartControl
                it[stringPreferencesKey("adaptive_display")] = oldAdaptive
                it[stringPreferencesKey("hidden_apps")] = oldHidden
                it[stringPreferencesKey("onboarding")] = oldOnboarding
                it[stringPreferencesKey("security")] = oldSecurity
            }
            assertOldValuesSurvived()
        }

    @Test fun `old backups that still carry removed fields import`() =
        runBlocking {
            val backup =
                """
                {"appearance":$oldAppearance,"pages":$oldPages,"security":$oldSecurity,
                 "smartControl":$oldSmartControl,"adaptiveDisplay":$oldAdaptive,
                 "hiddenApps":$oldHidden,"onboarding":$oldOnboarding}
                """.trimIndent()
            assertTrue(repo.importAllSettings(backup))
            assertOldValuesSurvived()
        }
}
