# NoveLauncher — Codex Guide

A Hebrew-RTL Android launcher with on-device AI ranking, smart folders, themes, widgets, news feed, backups, and biometric app lock.

## Quick facts

- **App ID:** `com.ailauncher.app` (label "NoveLauncher")
- **Gradle root:** [android/](android/) — the repo root itself is **not** a Gradle project; open `android/` in Android Studio, not the repo root.
- **Module:** single Android app module `:app` (declared in [settings.gradle.kts](android/settings.gradle.kts))
- **SDK:** `minSdk 26`, `targetSdk 35`, `compileSdk 35`, Java/Kotlin target 17
- **AGP / Kotlin:** AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.12.01
- **Language:** Kotlin only. **i18n is complete, not partial** — 7 locales (he/en/ar/fr/ru/es/de), all kept key-for-key identical (verify with `sed -n 's/.*<string name="\([^"]*\)".*/\1/p' values-XX/strings.xml | sort` diffed across locales). Don't add inline Hebrew literals — add a string resource in all 7 files. `AppearanceSettings.appLanguage` has a real picker UI (Settings → Appearance → language chips) that calls `.recreate()` immediately after `AppCompatDelegate.setApplicationLocales(...)` — neither `LauncherActivity` nor `SettingsActivity` is an `AppCompatActivity`, so AppCompat's automatic recreate-on-locale-change doesn't apply here.
- **Git:** Tracked in git, origin = [github.com/DrummingBird1/NoveLauncher](https://github.com/DrummingBird1/NoveLauncher). `main` is the only branch. GitHub Actions runs three parallel jobs on every push/PR ([.github/workflows/android.yml](.github/workflows/android.yml)): `build` (assembleDebug/Release + lint + ktlint, all advisory except the assembles), `test` (unit tests + Jacoco + Paparazzi screenshot verification), and `instrumented-tests` (emulator-based, advisory/continue-on-error — new and unproven). [.github/workflows/release.yml](.github/workflows/release.yml) builds + drafts a GitHub Release (APK+AAB) on any `vX.Y.Z` tag push. Update flow: `git add . && git commit -m "…" && git push`.
- **Dependencies:** managed via Gradle version catalog ([gradle/libs.versions.toml](android/gradle/libs.versions.toml)), including `[bundles]` for grouped deps (e.g. `libs.bundles.compose`). Don't hardcode versions in build.gradle.kts. [.github/dependabot.yml](.github/dependabot.yml) opens weekly bump PRs, but ignores major-version bumps on `androidx.biometric`/`androidx.security:security-crypto` — those need a human reading the changelog, not an auto-mergeable PR.
- **Repo root layout:** three folders at the true repo root — `android/`, `assets/` and `docs/` (the published GitHub Pages site; see [ADR-0002](assets/docs/adr/0002-public-repo-docs-folder.md)). [android/](android/) is the entire Gradle/Android Studio project — open **this** folder in Android Studio. [assets/](assets/) holds everything else non-code: design source (`generate_graphics.py`, `graphics/`, `screenshots/`) and [assets/distribution/](assets/distribution/) (store listing text + [ALTERNATIVE_STORES.md](assets/distribution/ALTERNATIVE_STORES.md), a launch plan for stores other than Google Play). `.github/`, `CLAUDE.md`/`AGENTS.md` and the community health files (`README.md`, `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `CHANGELOG.md`) stay at the true repo root regardless — tooling and GitHub only read them from there.
- **Release signing** is opt-in via env vars read in [android/app/build.gradle.kts](android/app/build.gradle.kts) (`RELEASE_KEYSTORE_PATH/PASSWORD`, `RELEASE_KEY_ALIAS/PASSWORD`) — absent, `assembleRelease`/`bundleRelease` stay unsigned. **Debug builds are different**: they sign with a *committed* keystore, `android/app/debug.keystore` (fixed 2026-08-14) — before this fix, every CI runner generated its own random debug key, so every CI-built debug APK had a different signature and collided with whatever was previously installed ("App not installed as package conflicts with an existing package"). Don't regenerate `debug.keystore`; it needs to stay the same file forever.

## Build & run

```powershell
# From android/ (open THIS folder in Android Studio, not the repo root)
.\gradlew.bat assembleDebug        # APK
.\gradlew.bat bundleRelease        # AAB for Play Store
.\gradlew.bat installDebug         # install on connected device
.\gradlew.bat lint                 # AGP lint (advisory)
.\gradlew.bat ktlintCheck          # advisory
.\gradlew.bat testDebugUnitTest    # unit + Robolectric + Paparazzi tests
.\gradlew.bat verifyPaparazziDebug # screenshot tests only
```

Gradle Configuration Cache and `android.enableR8.fullMode` are both on ([gradle.properties](android/gradle.properties)) — a config-cache-incompatible plugin bump will fail loudly and clearly at build time, not silently.

## Architecture

Layered, Hilt-driven, single-Activity Compose UI. **Hilt everywhere**, including `SettingsActivity` (`@AndroidEntryPoint`) and `LauncherActivity` — there is no manual DI anywhere in this codebase. There are also no static singletons (`AILauncherApp.instance` was removed years ago) — `AppWidgetHost` and `IconCache` reach Composables via `LocalAppWidgetHost`/`LocalIconCache`, non-Compose code injects them from Hilt directly.

```
ui/                            ← Compose screens + ViewModel + Activity entry
  LauncherActivity.kt          ← FragmentActivity (needed for BiometricPrompt). Single host.
  LauncherViewModel.kt         ← @HiltViewModel, central state via StateFlow
  screens/                     ← HomeScreen, AppsScreen, NewsScreen, PersonalZoneScreen,
                                 StatisticsScreen, GlobalSearchScreen, OnboardingScreen,
                                 SettingsActivity.kt (separate Activity, its own sections
                                 split into AppearanceSettings.kt/BackupSettings.kt/etc.)
  components/                  ← LauncherWidgetProvider, LockLauncherTileService (QS tile)
  theme/                       ← Theme.kt, font + color mapping for ThemePreset

domain/
  models/                      ← settings + UI state types, split across 5 files (CoreModels,
                                 AppearanceModels, PlatformModels, etc.) — not one giant file
  models/AppCategory.kt        ← 11-category enum w/ Play Store mapping
  usecases/GetRankedAppsUseCase.kt  ← combines apps + usage + ml + notification badges
  repository/                  ← domain-layer interfaces for SettingsRepository and
                                 InstalledAppsRepository (impls in data/); AppBindsModule
                                 in di/ binds them via @Binds alongside AppModule's @Provides

data/
  SettingsRepository.kt        ← DataStore-backed, serializes everything as JSON strings
  InstalledAppsRepository.kt   ← PackageManager query, cached + invalidated by package broadcasts
  UsageStatsRepository.kt      ← UsageStatsManager events → snapshots
  AppCategoryProvider.kt       ← heuristic ranker (recency + freq + time-of-day + category)
  IconCache.kt                 ← memory LRU + disk-backed warm cache for launcher icons
  LauncherNotificationListener.kt  ← NotificationListenerService → unread badges
  db/LauncherDatabase.kt       ← Room: 5 entities + 2 DAOs, version 2 (indices added — see
                                 db/Migrations.kt for MIGRATION_1_2)
  backup/BackupManager.kt      ← local + Google Drive + OneDrive(disabled) + Box(disabled) + NAS
  backup/ScheduledBackupWorker.kt  ← WorkManager periodic backup, records success/failure +
                                 posts a failure notification
  iconpack/IconPackManager.kt  ← Nova/ADW/Lawnchair compatible appfilter.xml reader
  ml/AppPredictionEngine.kt    ← on-device prediction, designed to swap in TFLite later
  api/WeatherService.kt        ← Open-Meteo, direct HttpURLConnection + kotlinx.serialization

security/
  AppLockManager.kt            ← PIN/password/pattern/biometric per-app + launcher-wide lock
  BiometricHelper.kt           ← BiometricPrompt wrapper
  RootDetection.kt             ← heuristic, deterrent-only — see Pitfalls

di/AppModule.kt, AppBindsModule.kt  ← Hilt @Provides / @Binds for everything above
AILauncherApp.kt               ← @HiltAndroidApp + WorkManager Configuration.Provider
                                 + crash handler + optional Sentry init (see Pitfalls)
```

### Key flows

- **Home rendering pipeline**: `LauncherViewModel.refresh()` → `GetRankedAppsUseCase.execute()` → reads installed apps + usage events + notification badges → `AppCategoryProvider.rankApps()` (recency × 0.35 + freq × 0.30 + time-of-day × 0.25 + category boost × 0.10) → `autoGroup()` for smart folders → `IconCache.preloadFromDisk()` warms the icon cache before the state update → emits `AppListState`. Filtered through `hiddenApps`/`privateFolderPackages` before reaching UI.
- **App launch with lock check**: `LauncherActivity.launchAppWithLockCheck(pkg)` branches on `AppLockManager.getAppLockMethod()` — biometric goes to `BiometricHelper.authenticate()`, PIN/password/pattern surfaces a Compose `AlertDialog`. Unlock TTL is 5 minutes per package.
- **Settings persistence**: every settings group is serialized to JSON and stored as a string preference in a single `DataStore<Preferences>` under `launcher_settings`. `SettingsRepository` exposes one `Flow<T>` and one `suspend save*` per group.

### Conventions

- **UI strings live in `strings.xml`**, all 7 locales — see i18n note above. The only intentional inline literals are the 4 NewsSource Hebrew brand names (Walla/Haaretz/Kan/Calcalist — actual outlet names).
- **RTL-aware layout**: `android:supportsRtl="true"` in manifest, locale-sensitive padding via Compose.
- **Activity is `singleTask` + `category.HOME`** — this is a real home-screen launcher, not a regular app.
- **`SmartControlSettings.reduceMotion`** reaches Composables via `LocalReduceMotion` — check it before adding a new `AnimatedVisibility`/`animateScrollToPage`.

### Permissions (in [AndroidManifest.xml](android/app/src/main/AndroidManifest.xml))

Sensitive ones: `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `READ_CONTACTS`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS` (API 33+, requested at runtime from `LauncherActivity.onCreate`). `READ_EXTERNAL_STORAGE` only up to API 32. `CALL_PHONE`, `CAMERA`, and `ACCESS_FINE_LOCATION` were all removed years ago — don't re-add a dangerous permission without a real consumer.

## Pitfalls / non-obvious

1. **Crash handler lives in `AILauncherApp.installCrashHandler`**, installed once per process (not per-Activity) — records via a background `Thread` + `join(2s)` off the Main thread.
2. **Hebrew weekend in category boost**: `AppCategoryProvider.computeCategoryBoost()` treats Friday+Saturday as weekend. Hardcoded.
3. **UsageStats permission prompt fires once per process** — if denied, the user must grant it from Settings → Apps → NoveLauncher → Usage manually until the next app restart.
4. **OneDrive and Box backups are explicitly disabled** until Microsoft/Box OAuth flows are wired up — `BackupDestination` still lists them (so old exports don't break deserialization) but `backup()` returns a clear error and the Settings UI filters them out of the destination picker.
5. **Legacy credential migration runs on first verify**: unsalted-SHA-256/plaintext PIN/password values are silently upgraded to PBKDF2 on next successful verify. Same pattern for `SecuritySettings`/`BackupSettings` DataStore encryption.
6. **`LockLauncherTileService`'s Quick Settings tile "lock now" action revokes the unlock grace period, it does not disable the configured lock method** — flipping the method would silently remove the user's credential; expiring the grace window just forces re-auth.
7. **`AILauncherApp.BACKUP_CHANNEL_ID` is the app's first-ever NotificationChannel** — `ScheduledBackupWorker` is the only current poster, wrapped in `catch (_: SecurityException)` in case `POST_NOTIFICATIONS` was denied.
8. **Debug builds sign with the committed `android/app/debug.keystore`** — see Quick facts. Don't regenerate it.
9. **`IconCache`'s disk layer is write-only from the hot path** — `getOrLoad()` (called synchronously from Composables) never touches disk; a background coroutine persists on a cache miss, and `preloadFromDisk()` (suspend, called from `LauncherViewModel.refresh()`) is the only place that reads from disk, warming the memory cache before the UI needs it.
10. **`RootDetection.isLikelyRooted()` is a deterrent-only heuristic**, shown as an informational banner in Security settings — never used to gate or block anything, consistent with this app's documented "app lock is a deterrent, not a sandbox" security model.
11. **Sentry crash reporting is fully opt-in** via a `SENTRY_DSN` build-time env var (`AILauncherApp.initSentry`) — no DSN is committed to this repo, so it's a no-op by default. Same pattern as release signing.
12. **`LauncherDatabase` is at schema version 2** (indices added on `daily_stats`/`hourly_usage`) — `db/Migrations.kt`'s `MIGRATION_1_2` is a real (non-destructive) migration, tested against exported schema JSON in `android/app/schemas/` via `LauncherDatabaseMigrationTest`. Any future schema change needs both a new `Migration` and a version bump — don't rely on `fallbackToDestructiveMigrationOnDowngrade()`, which only covers downgrades.
13. **`GetRankedAppsUseCase` computes zero notification counts (not filtered-at-render)** when badges are off or snoozed — `RankedApp.notificationCount` is genuinely `0` in that state.
14. **`HttpURLConnection` can't do WebDAV `MKCOL` without a reflection workaround** (`setRequestMethod` only allow-lists standard verbs) — `BackupManager.tryCreateNasDirectory()` handles this, best-effort.

## Where to start common tasks

| Task | File |
| --- | --- |
| Change app-ranking math | [data/AppCategoryProvider.kt](android/app/src/main/java/com/ailauncher/app/data/AppCategoryProvider.kt) + [data/ml/AppPredictionEngine.kt](android/app/src/main/java/com/ailauncher/app/data/ml/AppPredictionEngine.kt) |
| Add a new settings group | model in [domain/models/](android/app/src/main/java/com/ailauncher/app/domain/models/), add Preference key + flow + save in [SettingsRepository.kt](android/app/src/main/java/com/ailauncher/app/data/SettingsRepository.kt), expose in [LauncherViewModel.kt](android/app/src/main/java/com/ailauncher/app/ui/LauncherViewModel.kt), wire UI in [SettingsActivity.kt](android/app/src/main/java/com/ailauncher/app/ui/screens/SettingsActivity.kt) |
| Add a launcher page | append a `LauncherPage` constant in domain/models, handle in the `when` inside `LauncherRoot` in [LauncherActivity.kt](android/app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt) |
| Add a theme preset | append to `ThemePreset.PRESETS` |
| Change the Room schema | add fields/entities in [data/db/LauncherDatabase.kt](android/app/src/main/java/com/ailauncher/app/data/db/LauncherDatabase.kt), bump `version`, add a `Migration` in [data/db/Migrations.kt](android/app/src/main/java/com/ailauncher/app/data/db/Migrations.kt), regenerate schema JSON with `./gradlew kspDebugKotlin`, add/extend `LauncherDatabaseMigrationTest` |
| Add a Play-Store-distributable graphic | regenerate via `assets/generate_graphics.py` — output lands in `assets/graphics/` |
| Add a backup destination | new branch in `BackupManager.backup()` + new entry in `BackupDestination` enum |
| Add a language | new `values-XX/strings.xml` with every key from `values-en/`, add the language to the picker list in [AppearanceSettings.kt](android/app/src/main/java/com/ailauncher/app/ui/screens/AppearanceSettings.kt) |

## Testing

`junit` + `mockk` + `kotlinx-coroutines-test` + `robolectric` + Compose UI test + Paparazzi (screenshot) + MockWebServer + Room testing are all on the classpath. Run via:

```powershell
# From android/
.\gradlew.bat testDebugUnitTest      # everything below, in one task
.\gradlew.bat verifyPaparazziDebug   # screenshot tests only
```

Key files: [AppCategoryProviderTest.kt](android/app/src/test/java/com/ailauncher/app/data/AppCategoryProviderTest.kt) (ranking math), [GetRankedAppsUseCaseTest.kt](android/app/src/test/java/com/ailauncher/app/domain/usecases/GetRankedAppsUseCaseTest.kt) (badge suppression), [IconCacheTest.kt](android/app/src/test/java/com/ailauncher/app/data/IconCacheTest.kt) (disk round-trip), [WeatherServiceTest.kt](android/app/src/test/java/com/ailauncher/app/data/api/WeatherServiceTest.kt) (MockWebServer contract test), [LauncherDatabaseMigrationTest.kt](android/app/src/test/java/com/ailauncher/app/data/db/LauncherDatabaseMigrationTest.kt) (Room migration), [HomeAppItemSnapshotTest.kt](android/app/src/test/java/com/ailauncher/app/ui/screens/HomeAppItemSnapshotTest.kt) (Paparazzi screenshots), [SettingsRepositoryTest.kt](android/app/src/test/java/com/ailauncher/app/data/SettingsRepositoryTest.kt) (real DataStore round-trip), [PortableBackupCryptoTest.kt](android/app/src/test/java/com/ailauncher/app/security/PortableBackupCryptoTest.kt) (encrypted-backup crypto).

A single instrumented (`androidTest`) smoke test exists ([ApplicationIdTest.kt](android/app/src/androidTest/java/com/ailauncher/app/ApplicationIdTest.kt)) plus an emulator CI job — both new and deliberately advisory-only until proven stable. Real Activity-level `androidTest` coverage would need `HiltTestApplication` wiring, not yet done.
