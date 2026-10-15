package com.ailauncher.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.ailauncher.app.domain.models.AppInfo

/**
 * v8 FIX (critical security regression): every UI entry point used to launch
 * straight through PackageManager, bypassing AppLockManager. These helpers find
 * the hosting [LauncherActivity] and go through its lock check; only outside it
 * (e.g. from SettingsActivity) do they fall back to a direct launch.
 *
 * v10: moved out of HomeScreen.kt; the [AppInfo] overload starts the exact
 * activity in the exact profile (work, Private Space) via LauncherApps.
 */
fun launchApp(context: Context, app: AppInfo) {
    val activity = context.findLauncherActivity()
    if (activity != null) activity.launchAppWithLockCheck(app) else launchDirect(context, app.packageName)
}

/** Personal-profile launch by package name (Personal Zone, news reader app). */
fun launchApp(context: Context, packageName: String) {
    val activity = context.findLauncherActivity()
    if (activity != null) activity.launchPackageWithLockCheck(packageName) else launchDirect(context, packageName)
}

fun Context.findLauncherActivity(): LauncherActivity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<LauncherActivity>().firstOrNull()

private fun launchDirect(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}
