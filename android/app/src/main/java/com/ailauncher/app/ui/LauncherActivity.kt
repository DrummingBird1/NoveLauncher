package com.ailauncher.app.ui

import android.app.AppOpsManager
import android.appwidget.AppWidgetHost
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ailauncher.app.AppLocales
import com.ailauncher.app.BuildConfig
import com.ailauncher.app.R
import com.ailauncher.app.data.IconCache
import com.ailauncher.app.data.launcher.AppLauncher
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.LockMethod
import com.ailauncher.app.security.BiometricHelper
import com.ailauncher.app.ui.screens.OnboardingScreen
import com.ailauncher.app.ui.screens.WhatsNewDialog
import com.ailauncher.app.ui.theme.AILauncherTheme
import com.ailauncher.app.ui.widgets.WidgetHostController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The home-screen Activity (singleTask + category.HOME).
 *
 * v10: split up. This file now only owns the Activity lifecycle and the
 * lock-checked launch entry points; the UI tree is [LauncherRoot], the unlock
 * prompt is [UnlockDialog], the add-widget flow is [WidgetHostController], and
 * CompositionLocals live in LauncherLocals.kt.
 */
@AndroidEntryPoint
class LauncherActivity : FragmentActivity() {
    @Inject lateinit var biometricHelper: BiometricHelper

    @Inject lateinit var widgetHost: AppWidgetHost

    @Inject lateinit var iconCache: IconCache

    @Inject lateinit var appLauncher: AppLauncher

    /** v10: generalised from "pending app" to any action waiting on a PIN/password/pattern. */
    private val pendingUnlock = mutableStateOf<PendingUnlock?>(null)

    /** v10: the latest Home press — LauncherRoot returns to the default page on each one. */
    val homeRequest = mutableStateOf<HomeRequest?>(null)

    private val widgets = WidgetHostController(this, { widgetHost }) { id, spanY -> viewModel?.addWidget(id, spanY) }

    // v9.3: without this, POST_NOTIFICATIONS (API 33+) is never granted, so
    // ScheduledBackupWorker's failure alert silently no-ops forever — see
    // Pitfall #20 in CLAUDE.md. A no-op on API < 33 or once answered.
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { /* Granted or denied — nothing more to do; a denial keeps backup alerts silent. */ }

    private var viewModel: LauncherViewModel? = null

    // Tracks whether we've already prompted the user for UsageStats this process.
    // Prevents an infinite redirect loop when the user denies the permission.
    private var usagePermissionRequested = false

    // v8: When the screen turns off, drop all cached unlocks so apps re-prompt on next entry.
    private val screenOffReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                c: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) viewModel?.appLockManager?.clearUnlockCache()
            }
        }

    // v8: Re-lock after returning from recents — anything resumed after >30s gone gets cleared.
    private var pausedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        // v8 FIX #11: splash before super.onCreate() — shows the logo while Hilt
        // initializes and the home screen renders its first frame.
        installSplashScreen().setKeepOnScreenCondition { viewModel?.settingsReady?.value != true }
        super.onCreate(savedInstanceState)
        widgets.onRestoreInstanceState(savedInstanceState)
        // v9: draw behind the system bars (enforced from targetSdk 35 anyway).
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: LauncherViewModel = hiltViewModel()
            viewModel = vm
            val appearance by vm.appearance.collectAsState()
            val onboarding by vm.onboarding.collectAsState()
            val smartControl by vm.smartControl.collectAsState()
            val settingsReady by vm.settingsReady.collectAsState()

            CompositionLocalProvider(
                LocalAppWidgetHost provides widgetHost,
                LocalIconCache provides iconCache,
                LocalAppLauncher provides appLauncher,
                LocalReduceMotion provides smartControl.reduceMotion,
            ) {
                AILauncherTheme(appearance = appearance) {
                    // v10: expose Compose testTags as resource ids so UiAutomator (the
                    // :baselineprofile generator/benchmark) can find elements by id.
                    androidx.compose.foundation.layout.Box(
                        androidx.compose.ui.Modifier
                            .semantics { testTagsAsResourceId = true },
                    ) {
                        if (!settingsReady) {
                            // Splash is still showing; draw nothing rather than defaults.
                        } else if (!onboarding.completed) {
                            OnboardingScreen(onComplete = { vm.completeOnboarding() })
                        } else {
                            LauncherRoot(viewModel = vm, activity = this@LauncherActivity)
                            pendingUnlock.value?.let { pending ->
                                UnlockDialog(pending, vm.appLockManager, onDismiss = { pendingUnlock.value = null })
                            }
                            if (onboarding.lastSeenVersionCode < BuildConfig.VERSION_CODE) {
                                WhatsNewDialog(onDismiss = { vm.markWhatsNewSeen(BuildConfig.VERSION_CODE) })
                            }
                        }
                    }
                }
            }

            LaunchedEffect(vm) {
                launch { vm.toastMessage.collectLatest { Toast.makeText(this@LauncherActivity, it, Toast.LENGTH_SHORT).show() } }
                vm.toastRes.collectLatest { Toast.makeText(this@LauncherActivity, getString(it), Toast.LENGTH_SHORT).show() }
            }

            // v9: lifecycle-gated periodic refresh — runs only while STARTED, so the
            // 15-min UsageStats requery + ML prediction don't fire while buried.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            LaunchedEffect(vm, lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    while (true) vm.periodicRefreshTick()
                }
            }
        }
    }

    // Below Android 13 the in-app language is applied here, per Activity (Pitfall #12).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.attach(this, newBase))
    }

    override fun onResume() {
        super.onResume()
        if (AppLocales.onResume(this)) return
        if (!hasUsageStatsPermission() && !usagePermissionRequested) {
            usagePermissionRequested = true
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: Exception) {
                Timber.w(e, "Could not open UsageStats settings")
            }
        }
        // Re-lock if we were paused for a while (likely user pressed Recents).
        if (pausedAt > 0 && System.currentTimeMillis() - pausedAt > RESUME_RELOCK_THRESHOLD_MS) {
            viewModel?.appLockManager?.clearUnlockCache()
        }
        try {
            widgetHost.startListening()
        } catch (e: Exception) {
            Timber.w(e, "widgetHost.startListening failed")
        }
        viewModel?.refresh()
        viewModel?.recordHomeImpressions()
    }

    override fun onPause() {
        super.onPause()
        pausedAt = System.currentTimeMillis()
        try {
            widgetHost.stopListening()
        } catch (e: Exception) {
            Timber.w(e, "widgetHost.stopListening failed")
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    /**
     * v10: Home pressed. Without this, pressing Home while on the Apps (or any
     * other) page left you there. Animated only when the launcher was already in
     * front — coming back from an app just lands on the default page.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        val alreadyOnHome =
            hasWindowFocus() &&
                (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0
        homeRequest.value = HomeRequest(animate = alreadyOnHome)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        widgets.onSaveInstanceState(outState)
    }

    /** Widget configuration activities report back here — see WidgetHostController. */
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (!widgets.onActivityResult(requestCode, resultCode)) super.onActivityResult(requestCode, resultCode, data)
    }

    fun launchWidgetPicker() = widgets.pick()

    // ── Lock-checked entry points ──────────────────────────────────────

    /** v10: every app launch from the launcher UI goes through here. */
    fun launchAppWithLockCheck(app: AppInfo) =
        withAppUnlocked(app.packageName) {
            if (!appLauncher.launch(app)) Toast.makeText(this, getString(R.string.toast_app_not_found), Toast.LENGTH_SHORT).show()
        }

    /** Personal-profile launch when only a package name is known (Personal Zone, news app). */
    fun launchPackageWithLockCheck(packageName: String) =
        withAppUnlocked(packageName) {
            appLauncher.launchPackage(packageName)
        }

    /** v10: app shortcuts honour the app's lock, same as the app itself. */
    fun launchShortcutWithLockCheck(
        app: AppInfo,
        shortcut: ShortcutInfo,
    ) = withAppUnlocked(app.packageName) {
        appLauncher.startShortcut(shortcut)
    }

    /**
     * v7 FIX #8: handles all lock paths — no lock → run [action] directly;
     * biometric → BiometricPrompt; PIN/password/pattern → [UnlockDialog].
     */
    private fun withAppUnlocked(
        packageName: String,
        action: () -> Unit,
    ) {
        val vm = viewModel ?: return
        lifecycleScope.launch {
            fun proceed() {
                vm.markAppOpened(packageName)
                action()
            }
            if (!vm.appLockManager.isAppLocked(packageName)) {
                proceed()
                return@launch
            }
            when (val method = vm.appLockManager.getAppLockMethod()) {
                LockMethod.FINGERPRINT, LockMethod.FACE -> {
                    biometricHelper.authenticate(
                        activity = this@LauncherActivity,
                        title = getString(R.string.auth_required_title),
                        subtitle = getString(R.string.auth_required_subtitle),
                        onSuccess = {
                            vm.appLockManager.grantBiometricUnlock(packageName)
                            proceed()
                        },
                        onError = { Toast.makeText(this@LauncherActivity, it, Toast.LENGTH_SHORT).show() },
                    )
                }

                LockMethod.PIN, LockMethod.PASSWORD, LockMethod.PATTERN -> {
                    pendingUnlock.value = PendingUnlock(packageName, method) { proceed() }
                }

                LockMethod.NONE -> {
                    proceed()
                }
            }
        }
    }

    fun authenticateLauncher(onSuccess: () -> Unit) {
        val vm = viewModel ?: return
        lifecycleScope.launch {
            if (vm.appLockManager.isLauncherUnlocked()) {
                onSuccess()
                return@launch
            }
            when (vm.appLockManager.getLauncherLockMethod()) {
                LockMethod.FINGERPRINT, LockMethod.FACE -> {
                    biometricHelper.authenticate(
                        activity = this@LauncherActivity,
                        title = getString(R.string.launcher_lock_title),
                        subtitle = getString(R.string.auth_required_subtitle),
                        onSuccess = {
                            vm.appLockManager.grantLauncherBiometricUnlock()
                            onSuccess()
                        },
                        onError = { Toast.makeText(this@LauncherActivity, it, Toast.LENGTH_SHORT).show() },
                    )
                }

                else -> {
                    onSuccess()
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private companion object {
        const val RESUME_RELOCK_THRESHOLD_MS = 30_000L
    }
}

/** A Home press; a new instance each time, so pressing Home twice still triggers twice. */
class HomeRequest(
    val animate: Boolean,
)
