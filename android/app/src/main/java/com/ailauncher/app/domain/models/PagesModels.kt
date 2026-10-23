package com.ailauncher.app.domain.models

import android.content.Context
import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.serialization.Serializable

/*
 * Launcher page model + per-page layout.
 */

/**
 * v9: Hybrid display name. Built-in pages set [displayNameRes] so the label follows
 * the active locale; custom pages created by the user leave it null and the
 * user-provided [displayName] is shown verbatim. Use [localizedName] to resolve.
 */
@Serializable data class LauncherPage(
    val id: String,
    val displayName: String,
    val iconName: String,
    val isBuiltIn: Boolean = true,
    val isRemovable: Boolean = true,
    @StringRes val displayNameRes: Int? = null,
) {
    fun localizedName(context: Context): String = displayNameRes?.let { context.getString(it) } ?: displayName

    companion object {
        val HOME = LauncherPage("home", "Home", "home", isRemovable = false, displayNameRes = R.string.page_home)
        val APPS = LauncherPage("apps", "Apps", "apps", displayNameRes = R.string.page_apps)
        val NEWS = LauncherPage("news", "News", "newspaper", displayNameRes = R.string.page_news)
        val PERSONAL = LauncherPage("personal", "Personal zone", "shield", displayNameRes = R.string.page_personal)
        val BUILT_IN = listOf(HOME, APPS, NEWS, PERSONAL)
    }
}

// dockApps: package names pinned from the long-press sheet (personal profile),
// at most PageLayoutSettings.MAX_DOCK_APPS. Only the "home" layout has a dock.
@Serializable data class PageLayoutSettings(
    val pageId: String = "home",
    val dockApps: List<String> = emptyList(),
) {
    companion object {
        const val MAX_DOCK_APPS = 5
    }
}

@Serializable data class PagesSettings(
    val enabledPages: List<LauncherPage> = listOf(LauncherPage.HOME, LauncherPage.APPS, LauncherPage.NEWS),
    val defaultPageId: String = "home",
    val customPages: List<LauncherPage> = emptyList(),
    val pageLayouts: Map<String, PageLayoutSettings> = emptyMap(),
)

/** The home dock's packages in order, keeping only those in [present] (installed, personal profile, not hidden). */
fun PagesSettings.dockPackages(present: Set<String>): List<String> =
    pageLayouts[LauncherPage.HOME.id]
        ?.dockApps
        .orEmpty()
        .filter { it in present }
        .distinct()

/**
 * Pins [packageName] to the home dock or, if it's already there, unpins it.
 * Packages missing from [present] are dropped on the way, so an uninstalled or
 * hidden app can't hold a slot nobody can see. A full dock is returned unchanged.
 */
fun PagesSettings.toggleDockApp(
    packageName: String,
    present: Set<String>,
): PagesSettings {
    val dock = dockPackages(present)
    val next =
        when {
            packageName in dock -> dock - packageName
            dock.size >= PageLayoutSettings.MAX_DOCK_APPS -> return this
            else -> dock + packageName
        }
    val home = pageLayouts[LauncherPage.HOME.id] ?: PageLayoutSettings()
    return copy(pageLayouts = pageLayouts + (LauncherPage.HOME.id to home.copy(dockApps = next)))
}
