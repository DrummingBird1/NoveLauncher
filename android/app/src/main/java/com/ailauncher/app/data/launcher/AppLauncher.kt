package com.ailauncher.app.data.launcher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.AppProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v10: starts apps and app shortcuts through [LauncherApps], so the right
 * activity opens in the right profile (personal, work, Private Space).
 *
 * This only *starts* things. The app-lock check happens before any of these
 * are called — see LauncherActivity.launchAppWithLockCheck.
 */
@Singleton
class AppLauncher @Inject constructor(@ApplicationContext private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private fun userOf(app: AppInfo): UserHandle =
        if (app.profile == AppProfile.PERSONAL) Process.myUserHandle()
        else userManager.getUserForSerialNumber(app.userSerial) ?: Process.myUserHandle()

    private fun componentOf(app: AppInfo) = ComponentName(app.packageName, app.activityName)

    /** Returns false if nothing could be started (app uninstalled, profile locked, …). */
    fun launch(app: AppInfo, sourceBounds: Rect? = null): Boolean {
        if (app.activityName.isNotBlank()) {
            try {
                launcherApps.startMainActivity(componentOf(app), userOf(app), sourceBounds, null)
                return true
            } catch (e: Exception) {
                Timber.w(e, "startMainActivity failed for %s", app.key)
            }
        }
        return launchPackage(app.packageName)
    }

    /** Personal-profile launch by package name only (Personal Zone, news app, tests). */
    fun launchPackage(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No launch activity for %s", packageName)
            false
        }
    }

    fun openAppInfo(app: AppInfo) {
        try {
            if (app.activityName.isNotBlank()) {
                launcherApps.startAppDetailsActivity(componentOf(app), userOf(app), null, null)
                return
            }
        } catch (e: Exception) {
            Timber.w(e, "startAppDetailsActivity failed for %s", app.key)
        }
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }

    /** Uninstall is offered for personal-profile apps only; other profiles go through App info. */
    fun requestUninstall(app: AppInfo) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE)
                    .setData(Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }

    /**
     * App shortcuts (static manifest + dynamic + pinned), e.g. "New chat".
     * Only the default home app may read them; returns empty otherwise.
     */
    fun shortcutsFor(app: AppInfo, max: Int = 5): List<ShortcutInfo> {
        if (!hasShortcutAccess()) return emptyList()
        return try {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
                .apply { if (app.activityName.isNotBlank()) setActivity(componentOf(app)) }
            (launcherApps.getShortcuts(query, userOf(app)) ?: emptyList())
                .filter { it.isEnabled }
                .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                .take(max)
        } catch (e: Exception) {
            // SecurityException if we lost the home role between the check and the call.
            Timber.w(e, "getShortcuts failed for %s", app.key)
            emptyList()
        }
    }

    fun shortcutIcon(shortcut: ShortcutInfo): Drawable? = try {
        launcherApps.getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi)
    } catch (_: Exception) { null }

    fun startShortcut(shortcut: ShortcutInfo): Boolean = try {
        launcherApps.startShortcut(shortcut, null, null)
        true
    } catch (e: Exception) {
        Timber.w(e, "startShortcut failed for %s", shortcut.id)
        false
    }

    fun hasShortcutAccess(): Boolean = try { launcherApps.hasShortcutHostPermission() } catch (_: Exception) { false }
}
