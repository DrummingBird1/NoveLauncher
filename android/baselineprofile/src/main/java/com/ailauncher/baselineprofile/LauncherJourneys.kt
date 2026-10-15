package com.ailauncher.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/** Shared UI-automation steps for the profile generator and the benchmark. */
internal const val TARGET_PACKAGE = "com.ailauncher.app"
private const val TIMEOUT_MS = 5_000L

/** Grants what would otherwise pop a system screen over the launcher on start. */
internal fun MacrobenchmarkScope.prepareLauncher() {
    device.executeShellCommand("appops set $TARGET_PACKAGE GET_USAGE_STATS allow")
    device.executeShellCommand("pm grant $TARGET_PACKAGE android.permission.POST_NOTIFICATIONS")
}

/**
 * First run shows onboarding. Its buttons carry test tags exposed as resource
 * ids (OnboardingScreen sets testTagsAsResourceId), so this works in any language.
 */
internal fun MacrobenchmarkScope.completeOnboardingIfShown() {
    repeat(10) {
        val finish = device.findObject(By.res("onboarding_finish"))
        if (finish != null) {
            finish.click()
            return
        }
        val next = device.findObject(By.res("onboarding_next")) ?: return
        next.click()
        device.waitForIdle()
    }
}

internal fun MacrobenchmarkScope.waitForHome() {
    device.wait(Until.hasObject(By.res("home_grid")), TIMEOUT_MS)
    device.waitForIdle()
}

/** Swipe to the Apps page and fling through the drawer. */
internal fun MacrobenchmarkScope.visitAppDrawer() {
    val home = device.findObject(By.res("home_grid")) ?: return
    home.swipe(Direction.UP, 0.8f)
    device.wait(Until.hasObject(By.res("apps_grid")), TIMEOUT_MS)
    device.findObject(By.res("apps_grid"))?.let { grid ->
        grid.setGestureMargin(device.displayWidth / 5)
        grid.fling(Direction.DOWN)
        grid.fling(Direction.UP)
    }
    device.pressBack()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.openSearch() {
    device.findObject(By.res("home_search"))?.click() ?: return
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}
