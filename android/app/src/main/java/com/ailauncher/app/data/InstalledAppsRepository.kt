package com.ailauncher.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Retrieves all launchable apps from the device.
 *
 * v9: in-memory cache + PackageManager BroadcastReceiver.
 *
 * Before v9, every LauncherViewModel.refresh() walked the full launchable-apps list:
 * queryIntentActivities → loadLabel + loadIcon for each result. On devices with
 * ~150 apps that's ~50 ms of synchronous PackageManager IPC + drawable inflation
 * on every onResume + every 15-minute periodic refresh. We now compute the list
 * once and hold it; an [appPackageReceiver] invalidates the cache only when the
 * OS tells us something actually changed (install / uninstall / update / replace).
 *
 * Singleton via Hilt — the receiver registers in init() and stays alive for the
 * full process lifetime, which matches the launcher Activity's expected lifetime
 * (home screen Activities are not normally killed during the user's session).
 */
class InstalledAppsRepository(
    private val context: Context,
    private val categoryProvider: AppCategoryProvider,
    private val iconCache: IconCache
) : com.ailauncher.app.domain.repository.InstalledAppsRepository {

    private val cache = AtomicReference<List<AppInfo>?>(null)
    private val refreshMutex = Mutex()

    /** Registered for the process lifetime — see class kdoc. */
    private val appPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            // Any package change invalidates the cached labels/icons/categories.
            // Don't try to surgically update individual entries — a relabel
            // (locale change on the source app) won't fire ACTION_PACKAGE_CHANGED
            // for every app, so a full re-walk on next get is the safe move.
            val action = intent?.action
            if (action != null) {
                Timber.d("Package broadcast: %s — invalidating cached app list", action)
                cache.set(null)
                // v9: also drop the changed package's icon so an app update doesn't
                // keep showing the old icon until LRU eviction. The broadcast data is
                // a `package:<name>` Uri.
                intent.data?.schemeSpecificPart
                    ?.let { pkg -> iconCache.invalidate(pkg) }
                    ?: iconCache.invalidateAll()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            // Package broadcasts carry a `package:` data URI — required filter scheme.
            addDataScheme("package")
        }
        try {
            // ContextCompat.registerReceiver picks the safer not-exported flag on
            // Android 13+ where receivers must opt out of system broadcasts.
            androidx.core.content.ContextCompat.registerReceiver(
                context, appPackageReceiver, filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            // Some restricted environments (work profile, locked-down OEMs)
            // refuse the registration. Fall back to no caching gracefully.
            Timber.w(e, "Could not register package broadcast receiver")
        }
    }

    /**
     * Force the next call to [getInstalledApps] to re-walk PackageManager.
     * Call after a manifest change that PackageManager doesn't broadcast (rare —
     * e.g. user-toggled accessibility services, language change).
     */
    override fun invalidate() { cache.set(null) }

    override suspend fun getInstalledApps(): List<AppInfo> {
        cache.get()?.let { return it }
        return refreshMutex.withLock {
            // Double-check inside the lock so concurrent refresh() callers
            // don't both pay the PackageManager walk.
            cache.get()?.let { return@withLock it }
            val fresh = computeFreshList()
            cache.set(fresh)
            fresh
        }
    }

    private suspend fun computeFreshList(): List<AppInfo> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

        resolveInfos.mapNotNull { ri ->
            val ai = ri.activityInfo ?: return@mapNotNull null
            val pkg = ai.packageName
            // skip ourselves
            if (pkg == context.packageName) return@mapNotNull null

            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

            val isSystem = appInfo != null &&
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            AppInfo(
                packageName = pkg,
                label = ri.loadLabel(pm).toString(),
                icon = ri.loadIcon(pm),
                category = categoryProvider.categorize(pkg, appInfo),
                isSystemApp = isSystem,
                installTime = try {
                    pm.getPackageInfo(pkg, 0).firstInstallTime
                } catch (_: Exception) { 0L },
                lastUpdateTime = try {
                    pm.getPackageInfo(pkg, 0).lastUpdateTime
                } catch (_: Exception) { 0L }
            )
        }
    }
}
