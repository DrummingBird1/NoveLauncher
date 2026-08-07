package com.ailauncher.app.domain.models

import android.content.Context
import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.serialization.Serializable

/**
 * Launcher page model + per-page layout + bottom-tab category configuration.
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
    @StringRes val displayNameRes: Int? = null
) {
    fun localizedName(context: Context): String =
        displayNameRes?.let { context.getString(it) } ?: displayName

    companion object {
        val HOME = LauncherPage("home", "Home", "home", isRemovable = false, displayNameRes = R.string.page_home)
        val APPS = LauncherPage("apps", "Apps", "apps", displayNameRes = R.string.page_apps)
        val NEWS = LauncherPage("news", "News", "newspaper", displayNameRes = R.string.page_news)
        val PERSONAL = LauncherPage("personal", "Personal zone", "shield", displayNameRes = R.string.page_personal)
        val BUILT_IN = listOf(HOME, APPS, NEWS, PERSONAL)
    }
}

// v5: Category bottom tabs
@Serializable data class CategoryTabSettings(
    val enabled: Boolean = true,
    val showBottomTabs: Boolean = true,
    val customCategories: List<CustomCategory> = emptyList(),
    val categoryIconSize: Int = 24,
    val categoryFontSize: Int = 11
)

@Serializable data class CustomCategory(
    val id: String,
    val name: String,
    val iconName: String,
    val packages: Set<String> = emptySet()
)

@Serializable data class PageLayoutSettings(
    val pageId: String = "home",
    val showTopBar: Boolean = true,
    val showBottomDock: Boolean = false,
    val dockApps: List<String> = emptyList(),
    val gridColumns: Int = 4
)

@Serializable data class PagesSettings(
    val enabledPages: List<LauncherPage> = listOf(LauncherPage.HOME, LauncherPage.APPS, LauncherPage.NEWS),
    val defaultPageId: String = "home",
    val customPages: List<LauncherPage> = emptyList(),
    val pageLayouts: Map<String, PageLayoutSettings> = emptyMap()
)
