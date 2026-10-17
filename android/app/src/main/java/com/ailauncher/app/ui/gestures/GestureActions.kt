package com.ailauncher.app.ui.gestures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import timber.log.Timber

/**
 * v10: what the home-screen gestures do. The settings for these existed since
 * v5 (SmartControlSettings.doubleTapToSleep / swipeDownNotifications /
 * swipeUpAppDrawer / gesturesEnabled) but nothing ever read them.
 */
object GestureActions {
    /** Swipe down → notification shade. */
    @SuppressLint("WrongConstant") // "statusbar" isn't a public Context constant
    fun expandNotifications(context: Context) {
        if (LockScreenAccessibilityService.openNotifications()) return
        // Fallback when the accessibility helper isn't enabled: the hidden-but-
        // unrestricted StatusBarManager.expandNotificationsPanel() (needs the
        // normal EXPAND_STATUS_BAR permission). Every major third-party launcher
        // relies on it; if an OEM blocks it, the gesture simply does nothing.
        try {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        } catch (e: Exception) {
            Timber.w(e, "expandNotificationsPanel unavailable")
        }
    }

    /** Double tap → screen off. Returns false if the accessibility helper isn't connected. */
    fun lockScreen(): Boolean = LockScreenAccessibilityService.lockScreen()

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Timber.w(e, "No accessibility settings screen")
        }
    }
}
