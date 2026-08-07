/*
 * v9: This file was a 320-line catch-all containing every settings type. Its
 * contents were split by domain so each file fits on one screen:
 *
 *   CoreModels.kt       — AppInfo, RankedApp, SmartFolder, UsageSnapshot,
 *                         LauncherSettings (top-level aggregator)
 *   AppearanceModels.kt — ThemeMode, ThemePreset, IconShape, LauncherFont,
 *                         ClockStyle, ClockSettings, SearchBarSettings,
 *                         Wallpaper*, LockScreenSettings, AppearanceSettings
 *   PagesModels.kt      — LauncherPage, CategoryTabSettings, CustomCategory,
 *                         PageLayoutSettings, PagesSettings
 *   SecurityModels.kt   — LockMethod, SecuritySettings, HiddenAppsSettings,
 *                         SmartControlSettings
 *   PlatformModels.kt   — Backup*, ScreenType, AdaptiveDisplaySettings,
 *                         RepairSettings, OnboardingState, WidgetSlot,
 *                         NewsSource, NewsSettings
 *
 * The package didn't change, so every `import com.ailauncher.app.domain.models.*`
 * keeps compiling without modification. This file is intentionally empty so
 * historic links in commits/docs still resolve to a real file in the tree.
 */
package com.ailauncher.app.domain.models
