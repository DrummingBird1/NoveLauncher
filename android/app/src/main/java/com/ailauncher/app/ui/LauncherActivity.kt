package com.ailauncher.app.ui

import android.app.AppOpsManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.appwidget.AppWidgetHost
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import timber.log.Timber
import com.ailauncher.app.R
import com.ailauncher.app.data.IconCache
import com.ailauncher.app.domain.models.LauncherPage
import com.ailauncher.app.domain.models.LockMethod
import com.ailauncher.app.domain.models.ScreenType
import com.ailauncher.app.security.BiometricHelper
import com.ailauncher.app.ui.screens.*
import com.ailauncher.app.ui.theme.AILauncherTheme
import com.ailauncher.app.ui.theme.launcherFontFamily
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v7 FIX #5: @AndroidEntryPoint added.
 * v7 FIX #8: Pending app launch with PIN/password dialog.
 */
/**
 * v8: AppWidgetHost is injected through Hilt and provided to Composables via
 * [LocalAppWidgetHost]. Non-Compose code paths use the [widgetHost] property.
 */
val LocalAppWidgetHost = staticCompositionLocalOf<AppWidgetHost> {
    error("LocalAppWidgetHost not provided")
}

/** v9: process-lifetime icon cache. See [IconCache]. */
val LocalIconCache = staticCompositionLocalOf<IconCache> {
    error("LocalIconCache not provided")
}

/** SmartControlSettings.reduceMotion, hoisted so any Composable can skip/shorten
 *  animations without threading the setting through every parameter list. */
val LocalReduceMotion = staticCompositionLocalOf { false }

@AndroidEntryPoint
class LauncherActivity : FragmentActivity() {

    @Inject lateinit var biometricHelperInjected: BiometricHelper
    val biometricHelper: BiometricHelper get() = biometricHelperInjected

    @Inject lateinit var widgetHost: AppWidgetHost

    @Inject lateinit var iconCache: IconCache

    // For dialog-based unlock flow
    private val _pendingUnlockApp = mutableStateOf<Pair<String, LockMethod>?>(null)

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                account?.let {
                    // v8 FIX: Only persist the account email. The real OAuth2 access token
                    // is fetched on-demand by BackupManager via GoogleAuthUtil.getToken().
                    // Previously we stored idToken/serverAuthCode as "oauth_token", which
                    // Drive's REST API rejects.
                    getSharedPreferences("cloud_auth", MODE_PRIVATE).edit()
                        .putString("account_email", it.email)
                        .remove("oauth_token")
                        .apply()
                    Toast.makeText(this, getString(R.string.toast_connected_to, it.email), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.toast_sign_in_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val wid = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: return@registerForActivityResult
            if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) viewModel?.addWidget(wid)
        }
    }

    private var viewModel: LauncherViewModel? = null

    // Tracks whether we've already prompted the user for UsageStats this process.
    // Prevents an infinite redirect loop when the user denies the permission.
    private var usagePermissionRequested = false

    // v8: When the screen turns off, drop all cached unlocks so apps re-prompt on next entry.
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel?.appLockManager?.clearUnlockCache()
            }
        }
    }

    // v8: Re-lock after returning from recents — anything resumed after >30s gone gets cleared.
    private var pausedAt: Long = 0L
    private val RESUME_RELOCK_THRESHOLD_MS = 30_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        // v8 FIX #11: Install splash screen BEFORE super.onCreate()
        // This shows the launcher logo briefly while Hilt initializes
        // and the home screen renders its first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // v9: draw behind the system bars. Android 15 (targetSdk 35) enforces this
        // anyway; calling it explicitly keeps behaviour consistent on older OS too.
        // Screens already apply statusBarsPadding()/navigationBarsPadding() insets.
        enableEdgeToEdge()

        // v9: Crash handler moved to AILauncherApp.installCrashHandler — it now
        // registers exactly once per process instead of leaking a wrapped handler
        // chain on every Activity recreation, and records via Thread+join(2s)
        // rather than runBlocking on the Main thread.

        setContent {
            val vm: LauncherViewModel = hiltViewModel()
            viewModel = vm
            val appearance by vm.appearance.collectAsState()
            val onboarding by vm.onboarding.collectAsState()
            val smartControl by vm.smartControl.collectAsState()

            CompositionLocalProvider(
                LocalAppWidgetHost provides widgetHost,
                LocalIconCache provides iconCache,
                LocalReduceMotion provides smartControl.reduceMotion
            ) {
                AILauncherTheme(appearance = appearance) {
                    if (!onboarding.completed) {
                        OnboardingScreen(onComplete = { vm.completeOnboarding() })
                    } else {
                        LauncherRoot(viewModel = vm, activity = this@LauncherActivity)
                        // v7 FIX #8: Show credential dialog when needed
                        CredentialDialog(viewModel = vm)
                        if (onboarding.lastSeenVersionCode < com.ailauncher.app.BuildConfig.VERSION_CODE) {
                            WhatsNewDialog(onDismiss = { vm.markWhatsNewSeen(com.ailauncher.app.BuildConfig.VERSION_CODE) })
                        }
                    }
                }
            }

            // Toasts
            LaunchedEffect(vm) {
                vm.toastMessage.collectLatest {
                    Toast.makeText(this@LauncherActivity, it, Toast.LENGTH_SHORT).show()
                }
            }

            // v9: lifecycle-gated periodic refresh. repeatOnLifecycle starts the
            // block when the Activity enters STARTED and cancels the coroutine on
            // STOPPED, so the 15-min UsageStats requery + ML prediction don't fire
            // while the launcher is buried under another foreground app.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            LaunchedEffect(vm, lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    while (true) {
                        vm.periodicRefreshTick()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        try { widgetHost.startListening() }
        catch (e: Exception) { Timber.w(e, "widgetHost.startListening failed") }
        viewModel?.refresh()
    }

    override fun onPause() {
        super.onPause()
        pausedAt = System.currentTimeMillis()
        try { widgetHost.stopListening() }
        catch (e: Exception) { Timber.w(e, "widgetHost.stopListening failed") }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(screenOffReceiver) } catch (_: IllegalArgumentException) {}
    }

    fun launchWidgetPicker() {
        val wid = widgetHost.allocateAppWidgetId()
        widgetPickerLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
        })
    }

    /**
     * v7 FIX #8: Handles all 3 lock paths:
     * - No lock → launch directly
     * - Biometric → BiometricPrompt
     * - PIN/password/pattern → set _pendingUnlockApp → triggers CredentialDialog
     */
    fun launchAppWithLockCheck(packageName: String) {
        val vm = viewModel ?: return
        lifecycleScope.launch {
            if (!vm.appLockManager.isAppLocked(packageName)) {
                vm.markAppOpened(packageName)
                launchAppDirect(packageName); return@launch
            }
            val method = vm.appLockManager.getAppLockMethod()
            when (method) {
                LockMethod.FINGERPRINT, LockMethod.FACE -> {
                    biometricHelper.authenticate(
                        activity = this@LauncherActivity,
                        title = getString(R.string.auth_required_title),
                        subtitle = getString(R.string.auth_required_subtitle),
                        onSuccess = {
                            vm.appLockManager.grantBiometricUnlock(packageName)
                            vm.markAppOpened(packageName)
                            launchAppDirect(packageName)
                        },
                        onError = { Toast.makeText(this@LauncherActivity, it, Toast.LENGTH_SHORT).show() }
                    )
                }
                LockMethod.PIN, LockMethod.PASSWORD, LockMethod.PATTERN -> {
                    _pendingUnlockApp.value = packageName to method
                }
                LockMethod.NONE -> { vm.markAppOpened(packageName); launchAppDirect(packageName) }
            }
        }
    }

    fun authenticateLauncher(onSuccess: () -> Unit) {
        val vm = viewModel ?: return
        lifecycleScope.launch {
            if (vm.appLockManager.isLauncherUnlocked()) { onSuccess(); return@launch }
            val method = vm.appLockManager.getLauncherLockMethod()
            when (method) {
                LockMethod.FINGERPRINT, LockMethod.FACE -> {
                    biometricHelper.authenticate(
                        activity = this@LauncherActivity,
                        title = getString(R.string.launcher_lock_title),
                        subtitle = getString(R.string.auth_required_subtitle),
                        onSuccess = { vm.appLockManager.grantLauncherBiometricUnlock(); onSuccess() },
                        onError = { Toast.makeText(this@LauncherActivity, it, Toast.LENGTH_SHORT).show() }
                    )
                }
                else -> onSuccess()
            }
        }
    }

    fun initiateGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    fun launchAppDirect(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(it)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    /**
     * v7 FIX #8: Compose credential dialog rendered inside Activity.
     */
    @Composable
    private fun CredentialDialog(viewModel: LauncherViewModel) {
        val pending = _pendingUnlockApp.value
        if (pending != null) {
            val (pkg, method) = pending
            var input by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            val wrongText = stringResource(R.string.auth_wrong_try_again)

            // v9: FLAG_SECURE while any unlock dialog is on screen — keeps the
            // credential entry out of screenshots and the recents thumbnail.
            // Scoped to the dialog's lifetime so the home screen stays screenshottable.
            DisposableEffect(Unit) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                onDispose { window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
            }

            // v9: PATTERN unlock previously fell through to `else -> false` in the
            // verify branch below, so a pattern-locked app could never be opened —
            // the user locked themselves out. Render the 3x3 grid and verify it.
            if (method == LockMethod.PATTERN) {
                PatternDialog(
                    onDismiss = { _pendingUnlockApp.value = null },
                    title = stringResource(R.string.auth_required_title)
                ) { drawn ->
                    scope.launch {
                        if (viewModel.appLockManager.verifyPattern(drawn)) {
                            viewModel.appLockManager.grantAppUnlock(pkg)
                            viewModel.markAppOpened(pkg)
                            _pendingUnlockApp.value = null
                            launchAppDirect(pkg)
                        } else {
                            Toast.makeText(this@LauncherActivity, wrongText, Toast.LENGTH_SHORT).show()
                            _pendingUnlockApp.value = null
                        }
                    }
                }
                return
            }

            AlertDialog(
                onDismissRequest = { _pendingUnlockApp.value = null },
                title = { Text(stringResource(R.string.auth_required_title)) },
                text = {
                    Column {
                        Text(
                            stringResource(
                                if (method == LockMethod.PIN) R.string.auth_app_locked_with_pin
                                else R.string.auth_app_locked_with_password
                            ),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it; error = null },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            isError = error != null
                        )
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val ok = when (method) {
                                LockMethod.PIN -> viewModel.appLockManager.verifyPin(input)
                                LockMethod.PASSWORD -> viewModel.appLockManager.verifyPassword(input)
                                else -> false
                            }
                            if (ok) {
                                viewModel.appLockManager.grantAppUnlock(pkg)
                                viewModel.markAppOpened(pkg)
                                _pendingUnlockApp.value = null
                                launchAppDirect(pkg)
                            } else {
                                error = wrongText
                                input = ""
                            }
                        }
                    }) { Text(stringResource(R.string.action_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { _pendingUnlockApp.value = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot(viewModel: LauncherViewModel, activity: LauncherActivity) {
    val pages by viewModel.pagesSettings.collectAsState()
    val newsSettings by viewModel.newsSettings.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val enabledPages = pages.enabledPages.ifEmpty { listOf(LauncherPage.HOME) }
    val defaultIndex = enabledPages.indexOfFirst { it.id == pages.defaultPageId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = defaultIndex, pageCount = { enabledPages.size })
    val scope = rememberCoroutineScope()
    var showNavBar by remember { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val adaptiveDisplay by viewModel.adaptiveDisplay.collectAsState()

    val effectiveColumns = when {
        !adaptiveDisplay.autoDetect -> when (adaptiveDisplay.forcedType) {
            ScreenType.TABLET -> adaptiveDisplay.tabletColumns
            ScreenType.FOLDABLE -> adaptiveDisplay.foldableColumns
            ScreenType.LANDSCAPE -> adaptiveDisplay.landscapeColumns
            ScreenType.PHONE -> appearance.gridColumns
        }
        config.screenWidthDp >= 840 -> adaptiveDisplay.tabletColumns
        config.screenWidthDp >= 600 -> adaptiveDisplay.foldableColumns
        else -> appearance.gridColumns
    }

    // v8: Predictive back. On Android 14+ the system pre-renders the back gesture
    // animation while we animate the pager to the home page. Earlier versions just see
    // the animated scroll. The PredictiveBackHandler also lets the user cancel mid-swipe.
    androidx.activity.compose.PredictiveBackHandler(
        enabled = pagerState.currentPage != defaultIndex
    ) { progress ->
        try {
            progress.collect { /* progress.progress is 0..1 — could fade UI here */ }
            // Gesture completed.
            scope.launch { pagerState.animateScrollToPage(defaultIndex) }
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            // Gesture cancelled by user — stay on current page.
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < enabledPages.size) viewModel.navigateToPage(enabledPages[pagerState.currentPage])
    }
    LaunchedEffect(showNavBar) { if (showNavBar) { kotlinx.coroutines.delay(3000); showNavBar = false } }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30) showNavBar = true
                    if (dragAmount > 30) showNavBar = false
                }
            }
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val page = enabledPages.getOrNull(pageIndex)
            when (page?.id) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    gridColumns = effectiveColumns,
                    onSwipeUp = {
                        // v8: Swipe-up from Home jumps to the Apps page if enabled.
                        val appsIdx = enabledPages.indexOfFirst { it.id == "apps" }
                        if (appsIdx >= 0) scope.launch { pagerState.animateScrollToPage(appsIdx) }
                    }
                )
                "apps" -> AppsScreen(viewModel = viewModel, gridColumns = effectiveColumns)
                "news" -> NewsScreen(newsSettings = newsSettings)
                "personal" -> PersonalZoneScreen(viewModel = viewModel)
                else -> Box(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    Text(page?.localizedName(ctx) ?: "", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        val reduceMotion = LocalReduceMotion.current
        AnimatedVisibility(
            visible = showNavBar && enabledPages.size > 1,
            enter = if (reduceMotion) EnterTransition.None else slideInVertically { it },
            exit = if (reduceMotion) ExitTransition.None else slideOutVertically { it }
        ) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
                enabledPages.forEachIndexed { index, page ->
                    val pageName = page.localizedName(ctx)
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(when (page.iconName) { "home" -> Icons.Rounded.Home; "apps" -> Icons.Rounded.Apps; "newspaper" -> Icons.Rounded.Newspaper; "shield" -> Icons.Rounded.Shield; else -> Icons.Rounded.Tab }, pageName) },
                        label = { Text(pageName, fontSize = 11.sp, fontFamily = launcherFontFamily(appearance.pageFont)) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}
