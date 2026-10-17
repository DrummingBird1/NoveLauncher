package com.ailauncher.app.ui

import android.appwidget.AppWidgetHost
import androidx.compose.runtime.staticCompositionLocalOf
import com.ailauncher.app.data.IconCache
import com.ailauncher.app.data.launcher.AppLauncher

/*
 * CompositionLocals provided once in LauncherActivity.setContent.
 * v10: split out of LauncherActivity.kt.
 */

/** v8: AppWidgetHost is injected through Hilt; non-Compose code uses the Activity's field. */
val LocalAppWidgetHost =
    staticCompositionLocalOf<AppWidgetHost> {
        error("LocalAppWidgetHost not provided")
    }

/** v9: process-lifetime icon cache. See [IconCache]. */
val LocalIconCache =
    staticCompositionLocalOf<IconCache> {
        error("LocalIconCache not provided")
    }

/** v10: LauncherApps-backed launching, App info and app shortcuts. */
val LocalAppLauncher =
    staticCompositionLocalOf<AppLauncher> {
        error("LocalAppLauncher not provided")
    }

/** SmartControlSettings.reduceMotion, hoisted so any Composable can skip/shorten
 *  animations without threading the setting through every parameter list. */
val LocalReduceMotion = staticCompositionLocalOf { false }
