# NoveLauncher — Codex Guide

A Hebrew-RTL Android launcher with on-device AI ranking, smart folders, themes, widgets, news feed, backups, and biometric app lock.

## Quick facts

- **App ID:** `com.ailauncher.app` (label "NoveLauncher")
- **Gradle root:** `D:\AI\Codex\NoveLauncher\` (flattened in v8; the previous `NoveLauncher-v8\v8\` nesting was removed)
- **Module:** single Android app module `:app` (declared in [settings.gradle.kts](settings.gradle.kts))
- **SDK:** `minSdk 26`, `targetSdk 35`, `compileSdk 35`, Java/Kotlin target 17
- **Version:** `versionCode 8`, `versionName "8.0.0"` ([build.gradle.kts:18-19](app/build.gradle.kts))
- **AGP / Kotlin:** AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.12.01
- **Language:** Kotlin only. v9 finished the i18n extraction: ~300 UI strings now resolve through `strings.xml` with full translations in en/ar/fr/ru/he. Enums and `LauncherPage` use `@StringRes Int` displayNameRes (LauncherPage + ThemePreset have a hybrid pattern — built-in entries set the res id, user-created entries keep the typed String). The four NewsSource Hebrew brand names (Walla/Haaretz/Kan/Calcalist) are kept verbatim because they are the outlets' actual names. Anything left in Hebrew today is either a brand name, a comment, or the BuiltInWallpaper data (English placeholders pending a UI consumer).
- **Git:** Tracked in git, origin = [github.com/DrummingBird1/NoveLauncher](https://github.com/DrummingBird1/NoveLauncher). `main` is the only branch. GitHub Actions ([.github/workflows/android.yml](.github/workflows/android.yml)) runs `assembleDebug + lint + testDebugUnitTest` on every push/PR to main and uploads the debug APK + lint report as artefacts (14-day retention). Lint findings are non-blocking. Update flow: `git add . && git commit -m "…" && git push`.
- **Dependencies:** managed via Gradle version catalog ([gradle/libs.versions.toml](gradle/libs.versions.toml)). Don't hardcode versions in build.gradle.kts — bump in the catalog instead. Glance and Material 2 were dropped in v9; re-add via the catalog if/when needed.

## Build & run

```powershell
# From v8/
.\gradlew.bat assembleDebug        # APK
.\gradlew.bat bundleRelease        # AAB for Play Store
.\gradlew.bat installDebug         # install on connected device
.\gradlew.bat lint                 # AGP lint
.\gradlew.bat test                 # unit tests (none currently)
```

Open in Android Studio Ladybug+. The release build runs R8 shrinking ([build.gradle.kts:24-29](app/build.gradle.kts)) — ProGuard keep rules for serialization models live in [proguard-rules.pro](app/proguard-rules.pro).

## Architecture

Layered, Hilt-driven, single-Activity Compose UI.

```
ui/                            ← Compose screens + ViewModel + Activity entry
  LauncherActivity.kt          ← FragmentActivity (needed for BiometricPrompt). Single host.
  LauncherViewModel.kt         ← @HiltViewModel, central state via StateFlow
  screens/                     ← HomeScreen, AppsScreen, NewsScreen, PersonalZoneScreen,
                                 StatisticsScreen, GlobalSearchScreen, OnboardingScreen,
                                 SettingsActivity.kt (separate Activity)
  components/                  ← LauncherWidgetProvider (AppWidget receiver)
  theme/                       ← Theme.kt, font + color mapping for ThemePreset

domain/
  models/Models.kt             ← All @Serializable settings + UI state types in one file
  models/AppCategory.kt        ← 11-category enum w/ Play Store mapping
  usecases/GetRankedAppsUseCase.kt  ← combines apps + usage + ml + notification badges

data/
  SettingsRepository.kt        ← DataStore-backed, serializes everything as JSON strings
  InstalledAppsRepository.kt   ← PackageManager query for launchable apps
  UsageStatsRepository.kt      ← UsageStatsManager events → snapshots
  AppCategoryProvider.kt       ← heuristic ranker (recency + freq + time-of-day + category)
  LauncherNotificationListener.kt  ← NotificationListenerService → unread badges
  db/LauncherDatabase.kt       ← Room: 5 entities + 2 DAOs (UsageDao, NotificationDao)
  backup/BackupManager.kt      ← local + Google Drive + OneDrive + Box + NAS
  backup/ScheduledBackupWorker.kt  ← WorkManager periodic backup
  iconpack/IconPackManager.kt  ← Nova/ADW/Lawnchair compatible appfilter.xml reader
  ml/AppPredictionEngine.kt    ← on-device prediction, designed to swap in TFLite later
  api/WeatherService.kt        ← Open-Meteo, direct HttpURLConnection + kotlinx.serialization

security/
  AppLockManager.kt            ← PIN/password/pattern/biometric per-app + launcher-wide lock
  BiometricHelper.kt           ← BiometricPrompt wrapper

di/AppModule.kt                ← Hilt @Provides for everything above
AILauncherApp.kt               ← @HiltAndroidApp + WorkManager Configuration.Provider
                                 + AppWidgetHost (static instance — see Pitfalls)
```

### Key flows

- **Home rendering pipeline**: `LauncherViewModel.refresh()` → `GetRankedAppsUseCase.execute()` → reads installed apps + usage events + notification badges → `AppCategoryProvider.rankApps()` (recency × 0.35 + freq × 0.30 + time-of-day × 0.25 + category boost × 0.10) → `autoGroup()` for smart folders → emits `AppListState`. Filtered through `hiddenApps`/`privateFolderPackages` before reaching UI.
- **App launch with lock check**: `LauncherActivity.launchAppWithLockCheck(pkg)` branches on `AppLockManager.getAppLockMethod()` — biometric goes to `BiometricHelper.authenticate()`, PIN/password/pattern surfaces a Compose `AlertDialog` via `_pendingUnlockApp` mutableStateOf. Unlock TTL is 5 minutes per package.
- **Settings persistence**: every settings group (Appearance, Pages, Security, Backup, Widgets, News, HiddenApps, SmartControl, AdaptiveDisplay, Repair, Onboarding) is serialized to JSON and stored as a string preference in a single `DataStore<Preferences>` under `launcher_settings`. `SettingsRepository` exposes one `Flow<T>` and one `suspend save*` per group.

### Conventions

- **All UI strings are Hebrew, hardcoded inline** in Kotlin files (e.g. `"מומלצות עכשיו"`, `"אזור אישי"`). `res/values/strings.xml` only contains `app_name`. The `values-{ar,en,fr,ru}/strings.xml` folders exist but contain nothing useful. `AppearanceSettings.appLanguage` is read but never applied. **Don't assume strings come from resources.**
- **RTL-aware layout**: `android:supportsRtl="true"` in manifest, locale-sensitive padding via Compose.
- **Activity is `singleTask` + `category.HOME`** — this is a real home-screen launcher, not a regular app.
- **Hilt everywhere except `SettingsActivity`** which manually constructs `SettingsRepository`/`AppLockManager`/`BackupManager` ([SettingsActivity.kt:47-49](app/src/main/java/com/ailauncher/app/ui/screens/SettingsActivity.kt)). When adding code here, follow this manual pattern or refactor the whole Activity to use Hilt.
- **Static singletons**: `AILauncherApp.instance` (Application) and `widgetHost` (AppWidgetHost) are exposed via the companion. Used from non-DI sites — be careful with refactors.

### Permissions (in [AndroidManifest.xml](app/src/main/AndroidManifest.xml))

Sensitive ones: `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `READ_CONTACTS`, `CALL_PHONE`, `CAMERA`, `ACCESS_FINE_LOCATION`. `READ_EXTERNAL_STORAGE` only up to API 32 — see Pitfalls about backup storage on API 33+.

## Pitfalls / non-obvious

1. **Crash handler creates a fresh `SettingsRepository`** in the uncaught-exception path — bypasses Hilt by design (Hilt graph may already be torn down) but is fragile.
2. **Hebrew weekend in category boost**: `AppCategoryProvider.computeCategoryBoost()` treats Friday+Saturday as weekend. Hardcoded; don't surprise yourself when porting.
3. **UsageStats permission prompt fires once per process** ([LauncherActivity.onResume](app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt)) — if the user denies, they need to grant from Settings → Apps → NoveLauncher → Usage manually until the next app restart.
4. **OneDrive and Box backups are explicitly disabled** until Microsoft/Box OAuth flows are wired up; the manifest still lists them in `BackupDestination` but `backup()` returns a clear error.
5. **Legacy credential migration runs on first verify**: PIN/password values stored as unsalted SHA-256 (or plaintext, for `personalZonePin`) are accepted on verify and silently upgraded to PBKDF2 with per-install salt. Same pattern for `SecuritySettings` encryption — plain JSON in DataStore is migrated to AES-256-GCM-via-AndroidKeyStore on first save.
6. **`AppWidgetHost`** is provided to Composables via `LocalAppWidgetHost`. Non-Compose code injects `AppWidgetHost` directly from Hilt.
7. **`NewsCache`** is process-lifetime, 10-min TTL, single-entry.
8. **Timber DebugTree only in debug builds.** For release, plant a custom Tree.
9. **Failed-PIN lockout**: 5 wrong attempts → exponential backoff (30s → 60s → ...). Per-process; cleared on success and on app restart.
10. **App-lock cache invalidates on `ACTION_SCREEN_OFF`** and when `onResume` fires after >30 s of being paused. Apps re-prompt accordingly.
11. **i18n is partial**. Resources exist in `values/`, `values-en/`, `values-ar/`, `values-fr/`, `values-ru/` for ~30 common strings. The bulk of UI text is still inline Hebrew; extraction is straightforward but mechanical.
12. **`appLanguage` applied on Application.onCreate** via `AppCompatDelegate.setApplicationLocales`. Change requires app restart to fully propagate to all activities.

### Already fixed in v8 (don't redo)

#### Critical
- App lock bypass — every `launchApp()` call route through `LauncherActivity.launchAppWithLockCheck`
- Slider font-size shadow bug in HomeScreen clock dialog
- `screenOrientation="portrait"` lock removed
- Google Drive OAuth flow now uses real access tokens
- Local backups use `MediaStore.Downloads` on API 29+
- UsageStats infinite-redirect loop
- `versionCode` bumped to 8

#### Security
- PBKDF2-HMAC-SHA256 password hashing with per-install salt in EncryptedSharedPreferences
- `SecuritySettings` JSON encrypted with AES-256-GCM via AndroidKeyStore before DataStore
- Failed-attempt lockout with exponential backoff
- Unlock cache cleared on screen-off and on resume after >30 s pause
- Runtime check for `READ_CONTACTS` in `GlobalSearchScreen`
- Unused `CALL_PHONE` permission removed
- `personalZonePin` setter + verify
- `LauncherNotificationListener.scope` cancelled in `onDestroy`

#### Performance
- Bitmap conversion (`Drawable.toBitmap(...).asImageBitmap()`) memoised with `remember(packageName)`
- App search debounced via Flow (`debounce(180)`)
- `onResume` refresh throttled to 1 min
- RSS news cache (10-min TTL)
- TFLite dep removed (~3 MB)

#### Wiring (formerly dead features)
- `WeatherService` shows current temperature + emoji under the clock; auto-refreshes every 30 min
- `IconPackManager` listed in Settings → Icon Packs
- `StatisticsScreen` reachable from Settings → Usage Statistics
- `GlobalSearchScreen` opens from the home toolbar search button
- `appearance.appLanguage` applied via `AppCompatDelegate.setApplicationLocales`
- `ClockStyle.ANALOG` actually renders an analog clock face
- `PageLayoutSettings.dockApps` shows a bottom dock of up to 5 pinned apps on Home
- Swipe-up from Home navigates to the Apps page

#### UX / architecture
- Long-press menu on app icons (hide / private / lock / app info / uninstall)
- Predictive back animation (Android 14+)
- `BackHandler` → `PredictiveBackHandler`
- `AILauncherApp.instance` static removed; `AppWidgetHost` lives in Hilt
- `SettingsActivity` uses `@AndroidEntryPoint` + `@Inject`
- `Room` migration policy: destructive only on downgrade
- `importAllSettings` is atomic (single `DataStore.edit`)
- `WidgetView` keyed by `widgetId`, no longer rebuilds on scroll
- Notification badges reconcile against active notifications on connect + removal
- Timber added; key paths log instead of silently swallowing
- `MainScope().launch` → `lifecycleScope.launch`
- `OnboardingScreen` set-default uses `RoleManager.ROLE_HOME` on API 29+
- 6 unit tests for `AppCategoryProvider`
- `.gitignore` + corrupted `{gradle/...}` directory cleanup

## Where to start common tasks

| Task | File |
| --- | --- |
| Change app-ranking math | [data/AppCategoryProvider.kt](app/src/main/java/com/ailauncher/app/data/AppCategoryProvider.kt) (rule-based) + [data/ml/AppPredictionEngine.kt](app/src/main/java/com/ailauncher/app/data/ml/AppPredictionEngine.kt) (ML upgrade slot) |
| Add a new settings group | model in [domain/models/Models.kt](app/src/main/java/com/ailauncher/app/domain/models/Models.kt), add Preference key + flow + save in [SettingsRepository.kt](app/src/main/java/com/ailauncher/app/data/SettingsRepository.kt), expose in [LauncherViewModel.kt](app/src/main/java/com/ailauncher/app/ui/LauncherViewModel.kt), wire UI in [SettingsActivity.kt](app/src/main/java/com/ailauncher/app/ui/screens/SettingsActivity.kt) |
| Add a launcher page | append a `LauncherPage` constant in [domain/models/Models.kt](app/src/main/java/com/ailauncher/app/domain/models/Models.kt), handle in the `when` inside `LauncherRoot` ([LauncherActivity.kt:340-348](app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt)) |
| Add a theme preset | append to `ThemePreset.PRESETS` ([Models.kt:16](app/src/main/java/com/ailauncher/app/domain/models/Models.kt)) |
| Add a Play-Store-distributable graphic | regenerate via `playstore/generate_graphics.py` (PIL-based), see [playstore/](playstore/) for store listing markdown |
| Change icon | drawable XMLs in [app/src/main/res/drawable/](app/src/main/res/drawable/) + PNGs in [app/src/main/res/mipmap-*](app/src/main/res/) (themed monochrome in [ic_launcher_monochrome.xml](app/src/main/res/drawable/ic_launcher_monochrome.xml)) |
| Add a backup destination | new branch in `BackupManager.backup()` + new entry in `BackupDestination` enum |

## Testing

`junit` + `mockk` + `kotlinx-coroutines-test` are on the classpath. v8 adds [AppCategoryProviderTest.kt](app/src/test/java/com/ailauncher/app/data/AppCategoryProviderTest.kt) covering the scoring math and `autoGroup` filtering — start here for additional coverage of `AppPredictionEngine.computeFeatureScore` and the time-of-day affinity matrix. Run via:

```powershell
.\gradlew.bat testDebugUnitTest
```

Espresso is configured but no instrumentation tests have been written.
