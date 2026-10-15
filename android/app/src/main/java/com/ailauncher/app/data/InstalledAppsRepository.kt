package com.ailauncher.app.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.ailauncher.app.data.iconpack.IconPackManager
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.AppProfile
import com.ailauncher.app.domain.models.ProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Retrieves every launchable app the user can see, across all of their profiles.
 *
 * v10: built on [LauncherApps] instead of `PackageManager.queryIntentActivities`.
 * The PackageManager query only ever sees the current user, so:
 * - work-profile apps never appeared at all;
 * - Android 15's Private Space was invisible (it is only exposed to the default
 *   launcher, through LauncherApps, with ACCESS_HIDDEN_PROFILES);
 * - app shortcuts and per-profile launching weren't possible.
 *
 * Profiles in quiet mode (work apps paused, Private Space locked) contribute no
 * apps; [getProfiles] reports them so the UI can offer to turn them back on.
 *
 * v9 caching carries over: the list is computed once and held until something
 * actually changes. A [LauncherApps.Callback] (covers every profile, unlike the
 * old package broadcasts) and profile-availability broadcasts invalidate it,
 * and [changes] tells the ViewModel to refresh immediately — the user may
 * unlock Private Space without ever leaving the launcher, so waiting for the
 * next onResume isn't good enough.
 *
 * Singleton via Hilt — callbacks stay registered for the process lifetime.
 */
class InstalledAppsRepository(
    private val context: Context,
    private val categoryProvider: AppCategoryProvider,
    private val iconCache: IconCache,
    private val iconPackManager: IconPackManager
) : com.ailauncher.app.domain.repository.InstalledAppsRepository {

    private data class Snapshot(val iconPack: String, val apps: List<AppInfo>)

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val cache = AtomicReference<Snapshot?>(null)
    private val refreshMutex = Mutex()

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private fun onSomethingChanged(packageName: String?) {
        cache.set(null)
        // Drop the changed app's icons so an update doesn't keep showing the old one.
        if (packageName != null) iconCache.invalidatePackage(packageName) else iconCache.invalidateAll()
        _changes.tryEmit(Unit)
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = onSomethingChanged(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = onSomethingChanged(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = onSomethingChanged(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onSomethingChanged(null)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onSomethingChanged(null)
        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = onSomethingChanged(null)
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = onSomethingChanged(null)
    }

    /** Work profile paused/resumed, Private Space locked/unlocked. */
    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            Timber.d("Profile broadcast: %s", intent?.action)
            onSomethingChanged(null)
        }
    }

    init {
        try {
            launcherApps.registerCallback(launcherAppsCallback)
        } catch (e: Exception) {
            Timber.w(e, "LauncherApps.registerCallback failed")
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            }
        }
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                context, profileReceiver, filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            Timber.w(e, "Could not register profile broadcast receiver")
        }
    }

    override fun invalidate() { cache.set(null) }

    override suspend fun getInstalledApps(iconPackPackage: String): List<AppInfo> {
        cache.get()?.takeIf { it.iconPack == iconPackPackage }?.let { return it.apps }
        return refreshMutex.withLock {
            // Double-check inside the lock so concurrent callers don't both pay the walk.
            cache.get()?.takeIf { it.iconPack == iconPackPackage }?.let { return@withLock it.apps }
            val fresh = computeFreshList(iconPackPackage)
            cache.set(Snapshot(iconPackPackage, fresh))
            fresh
        }
    }

    override fun getProfiles(): List<ProfileState> = try {
        launcherApps.profiles
            .filter { it != Process.myUserHandle() }
            .map { user ->
                ProfileState(
                    profile = profileOf(user),
                    userSerial = userManager.getSerialNumberForUser(user),
                    isQuietModeEnabled = userManager.isQuietModeEnabled(user)
                )
            }
    } catch (e: Exception) {
        Timber.w(e, "getProfiles failed")
        emptyList()
    }

    /**
     * Pauses/resumes the work profile or locks/unlocks Private Space. The OS
     * shows its own credential prompt when unlocking requires it. Only the
     * default launcher (or a holder of MODIFY_QUIET_MODE) may call this, and
     * only on API 28+. Returns false if the request was refused.
     */
    override fun setQuietMode(userSerial: Long, enabled: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val user = userManager.getUserForSerialNumber(userSerial) ?: return false
        return try {
            userManager.requestQuietModeEnabled(enabled, user)
        } catch (e: SecurityException) {
            Timber.w(e, "requestQuietModeEnabled refused — not the default launcher?")
            false
        }
    }

    private suspend fun computeFreshList(iconPack: String): List<AppInfo> = withContext(Dispatchers.Default) {
        val profiles = try { launcherApps.profiles } catch (_: Exception) { listOf(Process.myUserHandle()) }
        profiles.flatMap { user ->
            if (user != Process.myUserHandle() && userManager.isQuietModeEnabled(user)) return@flatMap emptyList()
            val profile = profileOf(user)
            val serial = userManager.getSerialNumberForUser(user)
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                Timber.w(e, "getActivityList failed for %s", profile)
                emptyList()
            }
            activities.mapNotNull { lai ->
                val component = lai.componentName
                if (component.packageName == context.packageName && profile == AppProfile.PERSONAL) return@mapNotNull null
                toAppInfo(lai, component, profile, serial, iconPack)
            }
        }
    }

    private fun toAppInfo(
        lai: LauncherActivityInfo,
        component: ComponentName,
        profile: AppProfile,
        serial: Long,
        iconPack: String
    ): AppInfo {
        val appInfo: ApplicationInfo? = lai.applicationInfo
        val pkg = component.packageName
        // Icon packs (Nova/ADW/Lawnchair appfilter.xml) map a component to a
        // replacement drawable. Profile badges are only drawn on system icons —
        // a work app keeps its badge so it stays distinguishable.
        val packIcon = if (iconPack.isNotBlank() && profile == AppProfile.PERSONAL) {
            iconPackManager.getIconForApp(iconPack, "ComponentInfo{${component.packageName}/${component.className}}")
        } else null
        val icon = packIcon ?: try { lai.getBadgedIcon(0) } catch (_: Exception) { null }
        return AppInfo(
            packageName = pkg,
            label = lai.label?.toString() ?: pkg,
            icon = icon,
            category = categoryProvider.categorize(pkg, appInfo),
            isSystemApp = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            installTime = lai.firstInstallTime,
            lastUpdateTime = try {
                context.packageManager.getPackageInfo(pkg, 0).lastUpdateTime
            } catch (_: Exception) { 0L },
            activityName = component.className,
            profile = profile,
            userSerial = serial
        )
    }

    private fun profileOf(user: UserHandle): AppProfile {
        if (user == Process.myUserHandle()) return AppProfile.PERSONAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val type = try { launcherApps.getLauncherUserInfo(user)?.userType } catch (_: Exception) { null }
            return when (type) {
                UserManager.USER_TYPE_PROFILE_PRIVATE -> AppProfile.PRIVATE
                UserManager.USER_TYPE_PROFILE_CLONE -> AppProfile.CLONE
                else -> AppProfile.WORK
            }
        }
        // Before Android 15 the only other profile a launcher can see is a managed (work) profile.
        return AppProfile.WORK
    }
}
