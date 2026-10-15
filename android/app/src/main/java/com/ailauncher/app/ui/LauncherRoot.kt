package com.ailauncher.app.ui

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.LauncherPage
import com.ailauncher.app.domain.models.ScreenType
import com.ailauncher.app.domain.models.WallpaperMode
import com.ailauncher.app.ui.gestures.GestureActions
import com.ailauncher.app.ui.gestures.LockScreenAccessibilityService
import com.ailauncher.app.ui.screens.AppsScreen
import com.ailauncher.app.ui.screens.GlobalSearchScreen
import com.ailauncher.app.ui.screens.HomeScreen
import com.ailauncher.app.ui.screens.NewsScreen
import com.ailauncher.app.ui.screens.PersonalZoneScreen
import com.ailauncher.app.ui.theme.launcherFontFamily
import com.ailauncher.app.ui.wallpaper.WallpaperLayer
import kotlinx.coroutines.launch

/**
 * The launcher's page pager, bottom navigation and overlays. v10: moved out of
 * LauncherActivity.kt, and now also draws the wallpaper and wires the home
 * gestures (swipe up → Apps, swipe down → notifications, double tap → lock).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot(viewModel: LauncherViewModel, activity: LauncherActivity) {
    val pages by viewModel.pagesSettings.collectAsState()
    val newsSettings by viewModel.newsSettings.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val smartControl by viewModel.smartControl.collectAsState()
    val enabledPages = pages.enabledPages.ifEmpty { listOf(LauncherPage.HOME) }
    val defaultIndex = enabledPages.indexOfFirst { it.id == pages.defaultPageId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = defaultIndex, pageCount = { enabledPages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showNavBar by remember { mutableStateOf(false) }
    // v9.4: hoisted above the pager so the overlay renders regardless of the current page.
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showLockSetup by remember { mutableStateOf(false) }
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

    // v10: LIVE mode shows the system (live) wallpaper through a transparent window.
    val showSystemWallpaper = appearance.wallpaper.mode == WallpaperMode.LIVE
    DisposableEffect(showSystemWallpaper) {
        if (showSystemWallpaper) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        onDispose { }
    }

    // v8: Predictive back. On Android 14+ the system pre-renders the back gesture
    // animation while we animate the pager to the home page.
    androidx.activity.compose.PredictiveBackHandler(enabled = pagerState.currentPage != defaultIndex) { progress ->
        try {
            progress.collect { }
            scope.launch { pagerState.animateScrollToPage(defaultIndex) }
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            // Gesture cancelled by user — stay on current page.
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < enabledPages.size) viewModel.navigateToPage(enabledPages[pagerState.currentPage])
    }
    LaunchedEffect(showNavBar) { if (showNavBar) { kotlinx.coroutines.delay(3000); showNavBar = false } }

    val gestures = smartControl.gesturesEnabled
    val currentPageId = enabledPages.getOrNull(pagerState.currentPage)?.id ?: "home"

    Box(Modifier.fillMaxSize().background(if (showSystemWallpaper) Color.Transparent else MaterialTheme.colorScheme.background)) {
        WallpaperLayer(appearance.wallpaper, pageId = currentPageId, dimmed = currentPageId != "home")

        Column(
            Modifier.fillMaxSize()
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
                        onSwipeUp = if (gestures && smartControl.swipeUpAppDrawer) {
                            {
                                // v8: Swipe-up from Home jumps to the Apps page if enabled.
                                val appsIdx = enabledPages.indexOfFirst { it.id == "apps" }
                                if (appsIdx >= 0) scope.launch { pagerState.animateScrollToPage(appsIdx) }
                            }
                        } else null,
                        onSwipeDown = if (gestures && smartControl.swipeDownNotifications) {
                            { GestureActions.expandNotifications(context) }
                        } else null,
                        onDoubleTap = if (gestures && smartControl.doubleTapToSleep) {
                            { if (!GestureActions.lockScreen()) showLockSetup = true }
                        } else null,
                        onOpenSearch = { showGlobalSearch = true }
                    )
                    "apps" -> AppsScreen(viewModel = viewModel, gridColumns = effectiveColumns)
                    "news" -> NewsScreen(newsSettings = newsSettings)
                    "personal" -> PersonalZoneScreen(viewModel = viewModel)
                    else -> Box(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                        Text(page?.localizedName(context) ?: "", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            val reduceMotion = LocalReduceMotion.current
            AnimatedVisibility(
                visible = showNavBar && enabledPages.size > 1,
                enter = if (reduceMotion) EnterTransition.None else slideInVertically { it },
                exit = if (reduceMotion) ExitTransition.None else slideOutVertically { it }
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
                    enabledPages.forEachIndexed { index, page ->
                        val pageName = page.localizedName(context)
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            icon = {
                                Icon(
                                    when (page.iconName) {
                                        "home" -> Icons.Rounded.Home
                                        "apps" -> Icons.Rounded.Apps
                                        "newspaper" -> Icons.Rounded.Newspaper
                                        "shield" -> Icons.Rounded.Shield
                                        else -> Icons.Rounded.Tab
                                    },
                                    pageName
                                )
                            },
                            label = { Text(pageName, fontSize = 11.sp, fontFamily = launcherFontFamily(appearance.pageFont)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        if (showGlobalSearch) {
            GlobalSearchScreen(viewModel = viewModel, onDismiss = { showGlobalSearch = false })
        }
    }

    if (showLockSetup) LockGestureSetupDialog(onDismiss = { showLockSetup = false })
}

/**
 * v10: shown when double-tap-to-lock is on but the accessibility helper isn't
 * enabled. Doubles as Google Play's required prominent disclosure: it says what
 * the service does and doesn't do before sending the user to system settings.
 */
@Composable
fun LockGestureSetupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val supported = LockScreenAccessibilityService.isLockSupported
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gesture_lock_setup_title)) },
        text = {
            Text(stringResource(if (supported) R.string.gesture_lock_setup_body else R.string.gesture_lock_unsupported))
        },
        confirmButton = {
            if (supported) {
                TextButton(onClick = {
                    GestureActions.openAccessibilitySettings(context)
                    onDismiss()
                }) { Text(stringResource(R.string.gesture_lock_setup_open)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
