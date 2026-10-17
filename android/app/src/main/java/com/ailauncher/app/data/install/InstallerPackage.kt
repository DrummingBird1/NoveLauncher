package com.ailauncher.app.data.install

import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * The installer of record for this app — what [com.ailauncher.app.domain.install.InstallSource]
 * and [com.ailauncher.app.domain.install.RateTarget] map from. Null for adb installs, or when
 * PackageManager won't say. It can only change with an update, which restarts the process,
 * so callers read it once (UpdateChecker in a lazy, Settings → About in a remember).
 */
fun installerPackage(context: Context): String? =
    try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
    } catch (e: Exception) {
        Timber.w(e, "Couldn't read the installer package")
        null
    }
