package com.ailauncher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v10: records which classes/methods NoveLauncher touches on its critical
 * path — cold start into the home screen, scrolling the app drawer, opening
 * search — so ART can compile them ahead of time on install. Output is committed
 * at app/src/main/generated/baselineProfiles/baseline-prof.txt and shipped via
 * androidx.profileinstaller.
 *
 * Run: ./gradlew :app:generateBaselineProfile (device/emulator API 28+ attached).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        prepareLauncher()
        pressHome()
        startActivityAndWait()
        completeOnboardingIfShown()
        waitForHome()
        visitAppDrawer()
        openSearch()
    }
}
