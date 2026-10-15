package com.ailauncher.app.ui.gestures

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * v10: exists for exactly two global actions — turning the screen off (the
 * double-tap gesture) and pulling down the notification shade (the swipe-down
 * gesture, where it's more reliable than the hidden StatusBarManager API).
 *
 * It subscribes to no useful events and cannot read window content
 * (res/xml/lock_screen_accessibility.xml), and the user enables it themselves
 * from system settings after an in-app explanation. Google Play requires that
 * prominent disclosure plus an AccessibilityService declaration in Play Console
 * — see assets/distribution/PLAY_POLICY_NOTES.md.
 */
class LockScreenAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        @Volatile private var instance: WeakReference<LockScreenAccessibilityService>? = null

        /** GLOBAL_ACTION_LOCK_SCREEN exists from Android 9. */
        val isLockSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        val isConnected: Boolean get() = instance?.get() != null

        fun lockScreen(): Boolean =
            isLockSupported && instance?.get()?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        fun openNotifications(): Boolean =
            instance?.get()?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true

        /** Enabled in system settings (it may still be between enable and connect). */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val me = ComponentName(context, LockScreenAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
