package com.ailauncher.app.ui.screens

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.SmartControlSettings
import com.ailauncher.app.ui.gestures.LockScreenAccessibilityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Turning "Double tap → screen off" on while the accessibility helper is off must
 * show the Play prominent disclosure first, and only an explicit "Open accessibility
 * settings" (consent) may turn the switch on. See PLAY_POLICY_NOTES.md section 1.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class GesturesSettingsTest {
    @get:Rule val composeRule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun str(id: Int) = app.getString(id)

    private var settings by mutableStateOf(SmartControlSettings(gesturesEnabled = true, doubleTapToSleep = false))

    private fun show() =
        composeRule.setContent {
            GesturesSection(settings = settings, onUpdate = { settings = it })
        }

    // SwitchSetting rows add no semantics node, so a label can't scope its switch.
    // Order on the page: gestures on/off, swipe up, swipe down, double tap,
    // long-press vibration.
    private fun doubleTapSwitch() = composeRule.onAllNodes(isToggleable())[3]

    private fun dialogButton(id: Int) = composeRule.onNode(hasText(str(id)) and hasAnyAncestor(isDialog()))

    @Test fun `switching on without the helper shows the disclosure and Cancel leaves it off`() {
        show()
        doubleTapSwitch().assertIsOff() // the other three default on, so this is the right switch
        doubleTapSwitch().performClick()

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNode(hasText(str(R.string.gesture_lock_setup_body)) and hasAnyAncestor(isDialog())).assertExists()
        assertFalse("the switch must not turn on before consent", settings.doubleTapToSleep)

        dialogButton(R.string.action_cancel).performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        doubleTapSwitch().assertIsOff()
        assertFalse("Cancel is non-consent: the switch stays off", settings.doubleTapToSleep)
        assertNull("Cancel must not open system settings", shadowOf(app).nextStartedActivity)
    }

    @Test fun `consenting turns the switch on and opens accessibility settings`() {
        show()
        doubleTapSwitch().performClick()
        dialogButton(R.string.gesture_lock_setup_open).performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertTrue(settings.doubleTapToSleep)
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, shadowOf(app).nextStartedActivity?.action)
    }

    @Test fun `with the helper already enabled the switch turns on directly`() {
        val me = ComponentName(app, LockScreenAccessibilityService::class.java).flattenToString()
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, me)
        show()
        doubleTapSwitch().performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertTrue(settings.doubleTapToSleep)
    }

    @Test fun `switching off never shows the disclosure`() {
        settings = settings.copy(doubleTapToSleep = true)
        show()
        doubleTapSwitch().performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertFalse(settings.doubleTapToSleep)
    }

    @Test fun `the long-press vibration switch stays reachable with gestures off`() {
        settings = SmartControlSettings(gesturesEnabled = false, hapticFeedback = true)
        show()
        // Gestures off hides the three gesture switches: master switch, then vibration.
        val switches = composeRule.onAllNodes(isToggleable())
        switches.assertCountEquals(2)
        switches[1].performClick()

        assertFalse(settings.hapticFeedback)
    }
}
