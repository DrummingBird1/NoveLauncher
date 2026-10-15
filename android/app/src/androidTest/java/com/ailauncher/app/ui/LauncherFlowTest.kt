package com.ailauncher.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ailauncher.app.BuildConfig
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.domain.models.OnboardingState
import com.ailauncher.app.domain.models.SecuritySettings
import com.ailauncher.app.security.AppLockManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * v10: the first instrumented tests that exercise the real LauncherActivity —
 * Hilt graph, DataStore, Compose UI — on a device. The app-lock flow is the
 * one that most needs this: a regression there silently opens locked apps
 * (see "Security model" in CLAUDE.md).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LauncherFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var appLockManager: AppLockManager

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<LauncherActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        val pkg = context.packageName
        // No system dialogs over the activity: usage access and notifications pre-granted.
        InstrumentationRegistry.getInstrumentation().uiAutomation.apply {
            executeShellCommand("appops set $pkg GET_USAGE_STATS allow").close()
            executeShellCommand("pm grant $pkg android.permission.POST_NOTIFICATIONS").close()
        }
        runBlocking {
            settingsRepo.saveOnboarding(OnboardingState(completed = true, lastSeenVersionCode = BuildConfig.VERSION_CODE))
            settingsRepo.saveSecurity(SecuritySettings())
        }
        appLockManager.clearUnlockCache()
        scenario = ActivityScenario.launch(LauncherActivity::class.java)
    }

    @After
    fun tearDown() {
        runBlocking { settingsRepo.saveSecurity(SecuritySettings()) }
        scenario.close()
    }

    @Test
    fun homeScreen_showsRecommendedSection() {
        composeRule.onNodeWithText(context.getString(R.string.recommended_now)).assertIsDisplayed()
    }

    @Test
    fun lockedApp_promptsForPin_andOnlyTheRightPinUnlocks() {
        val target = "com.android.settings"
        runBlocking {
            appLockManager.setAppLockPin("2468")
            appLockManager.toggleAppLock(target)
            assertTrue(appLockManager.isAppLocked(target))
        }

        scenario.onActivity { it.launchPackageWithLockCheck(target) }
        composeRule.onNodeWithText(context.getString(R.string.auth_required_title)).assertIsDisplayed()

        val field = composeRule.onAllNodes(hasSetTextAction())[0]
        field.performTextInput("1111")
        composeRule.onNodeWithText(context.getString(R.string.action_ok)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.auth_wrong_try_again)).assertIsDisplayed()
        runBlocking { assertTrue("a wrong PIN must not unlock", appLockManager.isAppLocked(target)) }

        field.performTextInput("2468")
        composeRule.onNodeWithText(context.getString(R.string.action_ok)).performClick()
        composeRule.waitForIdle()
        runBlocking { assertFalse("the right PIN unlocks for the grace period", appLockManager.isAppLocked(target)) }
    }
}
