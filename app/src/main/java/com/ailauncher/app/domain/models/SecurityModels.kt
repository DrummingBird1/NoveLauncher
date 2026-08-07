package com.ailauncher.app.domain.models

import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.serialization.Serializable

/**
 * App lock + per-app hiding + gesture toggles. Anything that gates the user
 * away from content, plus the smart-control toggles tied to security UX
 * (haptic feedback, etc.).
 */

@Serializable enum class LockMethod(@StringRes val displayNameRes: Int) {
    NONE(R.string.lock_method_none),
    PIN(R.string.lock_method_pin),
    PASSWORD(R.string.lock_method_password),
    PATTERN(R.string.lock_method_pattern),
    FINGERPRINT(R.string.lock_method_fingerprint),
    FACE(R.string.lock_method_face)
}

@Serializable data class SecuritySettings(
    val appLockMethod: LockMethod = LockMethod.NONE,
    val appLockPin: String = "",
    val appLockPassword: String = "",
    // v9: pattern is now PBKDF2-hashed like PIN/password. [appLockPattern] (raw list)
    // is retained only to migrate legacy installs on the next successful verify; new
    // patterns write [appLockPatternHash] and leave the list empty.
    val appLockPattern: List<Int> = emptyList(),
    val appLockPatternHash: String = "",
    val lockedAppPackages: Set<String> = emptySet(),
    val isLayoutLocked: Boolean = false,
    val launcherLockMethod: LockMethod = LockMethod.NONE,
    val launcherLockPin: String = "",
    val launcherLockPassword: String = "",
    val personalZoneLockMethod: LockMethod = LockMethod.PIN,
    val personalZonePin: String = ""
)

@Serializable data class HiddenAppsSettings(
    val hiddenPackages: Set<String> = emptySet(),
    val privateFolderPackages: Set<String> = emptySet(),
    val privateFolderLocked: Boolean = true
)

// v5: Gesture settings with UI
@Serializable data class SmartControlSettings(
    val voiceControlEnabled: Boolean = false, val gesturesEnabled: Boolean = true,
    val doubleTapToSleep: Boolean = false, val swipeDownNotifications: Boolean = true,
    val swipeUpAppDrawer: Boolean = true, val pinchToOverview: Boolean = false,
    val shakeToSearch: Boolean = false, val smartScreenOff: Boolean = false,
    val hapticFeedback: Boolean = true,
    val longPressAction: String = "menu", // menu, hide, lock, info
    val homeButtonDoubleTap: String = "none", // none, search, recent, camera
    val reduceMotion: Boolean = false
)
