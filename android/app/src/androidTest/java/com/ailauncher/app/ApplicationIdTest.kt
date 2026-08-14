package com.ailauncher.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v9.3: first instrumented test in the project. Deliberately trivial — its job
 * is to validate the emulator CI pipeline itself (see the advisory
 * `instrumented-tests` job in .github/workflows/android.yml), not real app
 * behavior. LauncherActivity is @AndroidEntryPoint (Hilt) and singleTask/HOME,
 * so exercising it for real needs a HiltTestApplication test runner +
 * @HiltAndroidRule — a separate, larger addition, deliberately out of scope here.
 */
@RunWith(AndroidJUnit4::class)
class ApplicationIdTest {
    @Test
    fun targetContext_hasExpectedPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ailauncher.app", context.packageName)
    }
}
