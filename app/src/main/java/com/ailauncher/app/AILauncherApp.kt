package com.ailauncher.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * v6: Hilt-managed application class with WorkManager factory integration
 * for scheduled backup jobs.
 * v8: AppWidgetHost moved into Hilt's SingletonComponent (see AppModule); no longer
 * accessed via AILauncherApp.instance from non-DI sites.
 * v9: Crash handler installed exactly once at Application start (Application.onCreate
 * runs once per process), instead of re-registering on every Activity recreation.
 */
@HiltAndroidApp
class AILauncherApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var settingsRepoProvider: dagger.Lazy<com.ailauncher.app.data.SettingsRepository>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        installCrashHandler()
        applyConfiguredLanguage()
    }

    /**
     * Records uncaught exceptions to the repair counter, then chains to the
     * previous handler so Android still kills the process / shows the crash dialog.
     *
     * v9: previously this lived in LauncherActivity.onCreate, which meant:
     *  (a) on every recreation (config change, recreate()) we wrapped the previous
     *      handler, slowly building a chain that leaked Activity references; and
     *  (b) the recording path used runBlocking on whatever thread crashed —
     *      including the Main thread, where DataStore writes could deadlock if
     *      a coroutine on the Main dispatcher already held the file lock.
     *
     * Now: install once, off-Main, with a 2s budget so a slow disk can't hold the
     * crash dialog hostage.
     */
    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception on ${thread.name}")
            // Record on a fresh background thread with a hard timeout. We never
            // want crash bookkeeping to block the OS from showing the ANR dialog.
            val recorder = Thread {
                try {
                    runBlocking {
                        withTimeoutOrNull(2_000) {
                            settingsRepoProvider.get().recordCrash()
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to record crash to settings")
                }
            }
            recorder.start()
            try { recorder.join(2_000) } catch (_: InterruptedException) {}
            previous?.uncaughtException(thread, throwable)
        }
    }

    /**
     * v8: Apply [com.ailauncher.app.domain.models.AppearanceSettings.appLanguage] at process
     * start. Empty string = follow system. Otherwise we pass an explicit tag like "he", "en",
     * etc. through AndroidX's per-app locale API.
     */
    private fun applyConfiguredLanguage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tag = settingsRepoProvider.get().appearanceFlow.first().appLanguage
                if (tag.isBlank()) return@launch
                val locales = LocaleListCompat.forLanguageTags(tag)
                AppCompatDelegate.setApplicationLocales(locales)
            } catch (e: Exception) { Timber.w(e, "Could not apply configured language") }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        const val WIDGET_HOST_ID = 1024
    }
}
