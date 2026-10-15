package com.ailauncher.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * v10: runs instrumented tests inside HiltTestApplication instead of
 * AILauncherApp, so tests can inject the real singletons (@HiltAndroidTest +
 * HiltAndroidRule) and drive @AndroidEntryPoint activities like LauncherActivity.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
