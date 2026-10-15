package com.ailauncher.app.domain.repository

import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.ProfileState
import kotlinx.coroutines.flow.Flow

/**
 * v9: Domain-layer contract for the launchable-apps query. Real impl in
 * [com.ailauncher.app.data.InstalledAppsRepository], where the in-memory cache +
 * package broadcast receiver live. Tests can hand a fake list back without
 * touching PackageManager.
 */
interface InstalledAppsRepository {
    /**
     * Every launchable app across the user's available profiles. Cached; cheap on
     * subsequent calls. [iconPackPackage] blank = system icons (v10).
     */
    suspend fun getInstalledApps(iconPackPackage: String = ""): List<AppInfo>

    /** Non-personal profiles (work, Private Space, clone) and whether each is paused/locked. */
    fun getProfiles(): List<ProfileState>

    /** Pause/resume a profile (work) or lock/unlock it (Private Space). False if refused. */
    fun setQuietMode(userSerial: Long, enabled: Boolean): Boolean

    /** Emits whenever the app list or a profile's availability changed. */
    val changes: Flow<Unit>

    /** Force the next [getInstalledApps] to re-query. */
    fun invalidate()
}
