package com.ailauncher.app

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * v10: applies the in-app language choice (AppearanceSettings.appLanguage,
 * "" = follow the system).
 *
 * On Android 13+ this goes straight to the framework [LocaleManager].
 * AppCompatDelegate.setApplicationLocales only reaches LocaleManager through an
 * active AppCompatActivity, and neither of ours is one, so the language picker
 * silently did nothing there. The framework persists the choice and recreates
 * the app's activities itself.
 *
 * Below 13 the AppCompat call is kept as it was. It only takes effect in an
 * AppCompatActivity, so it is probably still a no-op there (see CLAUDE.md
 * Pitfall #12).
 */
object AppLocales {
    fun apply(
        context: Context,
        tag: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            val wanted = if (tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            // Unchanged → don't touch it, so a cold start never causes a configuration change.
            if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
        } else {
            AppCompatDelegate.setApplicationLocales(
                if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
            )
        }
    }
}
