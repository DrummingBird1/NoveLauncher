package com.ailauncher.app.domain.repository

import com.ailauncher.app.domain.models.AppInfo

/**
 * v9: Domain-layer contract for the launchable-apps query. Real impl in
 * [com.ailauncher.app.data.InstalledAppsRepository], where the in-memory cache +
 * package broadcast receiver live. Tests can hand a fake list back without
 * touching PackageManager.
 */
interface InstalledAppsRepository {
    /** Returns every launchable app on the device. Cached; cheap on subsequent calls. */
    suspend fun getInstalledApps(): List<AppInfo>

    /** Force the next [getInstalledApps] to re-walk PackageManager. */
    fun invalidate()
}
