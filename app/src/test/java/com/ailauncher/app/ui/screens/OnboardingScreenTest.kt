package com.ailauncher.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ailauncher.app.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric + Compose UI test — exercises real Compose semantics/click handling
 * on the JVM, no emulator/device needed. Kept deliberately narrow (one flow) since
 * this stack is new to this project; see CLAUDE.md testing notes for the tradeoffs
 * against full Espresso instrumentation tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class OnboardingScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun `skip button completes onboarding without visiting other steps`() {
        var completed = false
        composeRule.setContent {
            OnboardingScreen(onComplete = { completed = true })
        }

        val skipLabel = ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.action_skip)
        composeRule.onNodeWithText(skipLabel).performClick()

        assertTrue("onComplete should fire when Skip is tapped", completed)
    }

    @Test fun `next button advances past the welcome step without completing`() {
        var completed = false
        composeRule.setContent {
            OnboardingScreen(onComplete = { completed = true })
        }

        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val nextLabel = ctx.getString(R.string.action_next)
        val usageTitle = ctx.getString(R.string.onboarding_usage_title)

        composeRule.onNodeWithText(nextLabel).performClick()
        composeRule.waitForIdle()

        val found = composeRule.onAllNodesWithText(usageTitle).fetchSemanticsNodes().isNotEmpty()
        assertTrue("expected to land on the usage-access step after Next", found)
        assertTrue("onComplete should not fire from a single Next tap", !completed)
    }
}
