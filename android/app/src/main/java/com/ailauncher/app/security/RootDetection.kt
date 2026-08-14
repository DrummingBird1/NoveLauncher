package com.ailauncher.app.security

import android.os.Build
import java.io.File

/**
 * v9.3: heuristic-only, deterrent-only signal — consistent with the "App lock
 * / private folder / hidden apps are a deterrent, NOT a sandbox" security
 * model documented in CLAUDE.md. A rooted device can already bypass those
 * features entirely regardless of what this returns; it exists purely to
 * surface an informational warning in Security settings, never to gate or
 * block anything. Trivially defeated by root-hiding tools (Magisk
 * Hide/Zygisk, etc.) — that's expected and acceptable, since nothing here
 * depends on the check being unbeatable.
 */
object RootDetection {
    private val suPaths = listOf(
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
        "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
        "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
    )

    fun isLikelyRooted(): Boolean = hasSuBinary() || hasTestKeysBuildTag()

    private fun hasSuBinary(): Boolean =
        suPaths.any { runCatching { File(it).exists() }.getOrDefault(false) }

    private fun hasTestKeysBuildTag(): Boolean = Build.TAGS?.contains("test-keys") == true
}
