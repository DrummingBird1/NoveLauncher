# NoveLauncher — Claude Code Guide

A Hebrew-RTL Android launcher with on-device AI ranking, smart folders, themes, widgets, news feed, backups, and biometric app lock.

## Quick facts

- **App ID:** `com.ailauncher.app` (label "NoveLauncher")
- **Gradle root:** [android/](android/) (v9.1 — moved off the repo root so the repo root could hold just `android/` + `assets/`; flattened out of `NoveLauncher-v8/v8/` back in v8, for context on why this keeps getting revisited)
- **Module:** single Android app module `:app` (declared in [settings.gradle.kts](android/settings.gradle.kts))
- **SDK:** `minSdk 26`, `targetSdk 35`, `compileSdk 35`, Java/Kotlin target 17
- **Version:** `versionCode 10`, `versionName "9.1.0"` ([build.gradle.kts](android/app/build.gradle.kts))
- **AGP / Kotlin:** AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.12.01
- **Language:** Kotlin only. v9 finished the i18n extraction: ~300 UI strings now resolve through `strings.xml` with full translations in en/ar/fr/ru/he. Enums and `LauncherPage` use `@StringRes Int` displayNameRes (LauncherPage + ThemePreset have a hybrid pattern — built-in entries set the res id, user-created entries keep the typed String). The four NewsSource Hebrew brand names (Walla/Haaretz/Kan/Calcalist) are kept verbatim because they are the outlets' actual names. Anything left in Hebrew today is either a brand name, a comment, or the BuiltInWallpaper data (English placeholders pending a UI consumer).
- **Git:** Tracked in git, origin = [github.com/DrummingBird1/NoveLauncher](https://github.com/DrummingBird1/NoveLauncher). `main` is the only branch. GitHub Actions ([.github/workflows/android.yml](.github/workflows/android.yml)) runs `assembleDebug + lint + ktlintCheck + testDebugUnitTest + jacocoTestReport + assembleRelease` on every push/PR to main and uploads the debug APK, lint/ktlint/test/coverage reports as artefacts (14-day retention). Lint and ktlint findings are both non-blocking (`continue-on-error`) — the codebase predates both and hasn't been mass-reformatted. [.github/dependabot.yml](.github/dependabot.yml) opens weekly PRs for Gradle + Actions dependency bumps. Update flow: `git add . && git commit -m "…" && git push`.
- **Release signing** is opt-in via env vars read in [android/app/build.gradle.kts](android/app/build.gradle.kts) (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) — absent any of them, `assembleRelease` stays unsigned exactly like before. To actually sign a release: generate a keystore yourself (`keytool -genkeypair -v -keystore release.keystore -alias novelauncher -keyalg RSA -keysize 2048 -validity 10000`), keep it out of git, and either export those four env vars locally or add `RELEASE_KEYSTORE_BASE64` (base64 of the keystore file) + the three password/alias secrets to the GitHub repo's Actions secrets — CI decodes and signs automatically once present (see the "Decode release keystore" step in android.yml). No keystore is generated or stored by this repo.
- **Dependencies:** managed via Gradle version catalog ([gradle/libs.versions.toml](android/gradle/libs.versions.toml)). Don't hardcode versions in build.gradle.kts — bump in the catalog instead. Glance and Material 2 were dropped in v9; re-add via the catalog if/when needed.
- **Repo root layout (v9.1):** exactly two folders at the true repo root. [android/](android/) is the entire Gradle/Android Studio project (open **this** folder in Android Studio, not the repo root) — root build files, `gradle/`, and the single `:app` module live inside it. [assets/](assets/) holds everything else non-code: design source (`generate_graphics.py`, `graphics/`, `screenshots/`) and [assets/distribution/](assets/distribution/) (Play Store submission text — listing copy, privacy policy, terms, checklist). `.github/` stays at the true repo root regardless (GitHub only reads workflows/dependabot config from `<repo-root>/.github/`), as do `CLAUDE.md`/`AGENTS.md` (Claude Code / Codex only auto-load project instructions from the root they're opened in). Formerly one combined `playstore/` folder at the root alongside `android/app/`.

## Build & run

```powershell
# From android/ (open THIS folder in Android Studio, not the repo root)
.\gradlew.bat assembleDebug        # APK
.\gradlew.bat bundleRelease        # AAB for Play Store
.\gradlew.bat installDebug         # install on connected device
.\gradlew.bat lint                 # AGP lint
.\gradlew.bat test                 # unit tests
```

Open `android/` in Android Studio Ladybug+ (not the repo root — the repo root is no longer a Gradle project). The release build runs R8 shrinking ([build.gradle.kts:24-29](android/app/build.gradle.kts)) — ProGuard keep rules for serialization models live in [proguard-rules.pro](android/app/proguard-rules.pro).

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

- **UI strings live in `strings.xml`** (v9 i18n is complete — ~240 keys in he/en/ar/fr/ru). Add new strings as resources, not inline literals. Enums expose `@StringRes displayNameRes`; `LauncherPage`/`ThemePreset` use the hybrid `localizedName(context)` (built-in = res id, user-created = typed String). The only intentional inline Hebrew left is the four NewsSource brand names.
- **RTL-aware layout**: `android:supportsRtl="true"` in manifest, locale-sensitive padding via Compose.
- **Activity is `singleTask` + `category.HOME`** — this is a real home-screen launcher, not a regular app.
- **Hilt everywhere**, including `SettingsActivity` (`@AndroidEntryPoint` + `@Inject` since v8) and `LauncherActivity`. Repositories have domain-layer interfaces in `domain/repository/` (impls in `data/`), though `AppModule` still provides the concrete classes — bind the interface if you want to fake it in tests.
- **`SettingsActivity` is split** (v9): the Activity + `SettingsPage` enum + `SettingsRoot` Scaffold live in `SettingsActivity.kt`; each section is its own file (`AppearanceSettings.kt`, `PagesSettings.kt`, `SecuritySettings.kt`, `BackupSettings.kt`, `MiscSettings.kt`, shared widgets in `SettingsComponents.kt`). Same for `Models.kt` → 5 domain files.
- **`AppWidgetHost`** is provided by Hilt and reaches Composables via `LocalAppWidgetHost`; `IconCache` via `LocalIconCache`. The old `AILauncherApp.instance` static was removed in v8 — don't reintroduce static singletons.
- **`SmartControlSettings.reduceMotion`** reaches Composables via `LocalReduceMotion` (provided once in `LauncherActivity.setContent`, default `false`). Check it before adding a new `AnimatedVisibility`/`animateScrollToPage` — swap to `EnterTransition.None`/`ExitTransition.None`/`scrollToPage` when `true`, same pattern as `OnboardingScreen` and `AppsScreen`'s folder expand animation.
- **`IconCache`** sizes itself off `ActivityManager.getMemoryClass()` (1/8th, clamped to [4 MB, 32 MB]) instead of a hardcoded constant — see kdoc in `IconCache.kt` if you need to retune the fraction/clamp.
- **`Theme.kt`** splits into `AILauncherTheme` (decides dynamic-vs-preset) + `buildPresetColorScheme` (the original preset/custom-color math, unchanged). `AppearanceSettings.useDynamicColor` opts into `dynamicLightColorScheme`/`dynamicDarkColorScheme` on API 31+ — ignored when `useCustomColors` is also on (custom colors should win) or below API 31 (the toggle itself is hidden from Settings on older devices).

### Permissions (in [AndroidManifest.xml](android/app/src/main/AndroidManifest.xml))

Sensitive ones: `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `READ_CONTACTS`, `ACCESS_COARSE_LOCATION`. `READ_EXTERNAL_STORAGE` only up to API 32 — see Pitfalls about backup storage on API 33+. v8 removed `CALL_PHONE`; v9 removed `CAMERA` (camera button uses an intent) and `ACCESS_FINE_LOCATION` (weather only needs COARSE). Don't re-add a dangerous permission without a real API consumer — Play flags unused ones.

## Security model (read before touching lock/crypto code)

- **App-lock / private folder / hidden apps are a deterrent, NOT a sandbox.** They only intercept launches routed through `LauncherActivity.launchAppWithLockCheck`. A "locked" app is still reachable from recents, notifications, another launcher, `adb am start`, app links, widgets, or the assistant. This is inherent to every third-party launcher — never describe it as a security guarantee. Real isolation needs an OS-level mechanism (work profile / Android per-app lock), which a launcher can't provide.
- **At rest**: `SecuritySettings` and `BackupSettings` are AES-256-GCM encrypted (AndroidKeyStore, `SecureCrypto`) before DataStore — the rest of the groups are plain JSON. PIN/password/**pattern** are PBKDF2-HMAC-SHA256 (`appLockPatternHash`). Legacy unsalted-SHA-256 and plaintext patterns migrate on first successful verify.
- **Brute-force**: failed-attempt count + exponential lockout are persisted to EncryptedSharedPreferences (`secure_lock_prefs`), so force-stop can't reset them. PBKDF2 is 100k iterations — deliberately not raised, because a 4–6 digit PIN is keyspace-bound, not iteration-bound; the lockout is the real defense.
- **Backups**: `exportAllSettings` blanks `nasPassword` so portable `.json` files never carry it. `dataExtractionRules`/`backup_rules.xml` exclude `cloud_auth`, `secure_lock_prefs`, and `launcher.db` from auto-backup. Everything else in an export is still plain JSON unless the user opts into a backup password — `exportAllSettingsEncrypted(password)`/`importAllSettingsEncrypted(content, password)` wrap/unwrap the export in `security/PortableBackupCrypto.kt` (PBKDF2-derived AES-256-GCM, **not** AndroidKeyStore-bound, since the file must decrypt on a different device). Envelope is tagged with a `"NVLBK1:"` prefix so import can tell an encrypted file from a legacy plain-JSON one without guessing; `importAllSettings`/`restoreFromJson` (no password param) still only accept plain JSON, unchanged, for existing callers.

## Pitfalls / non-obvious

1. **Crash handler lives in `AILauncherApp.installCrashHandler`** (v9) — registered once per process, records via a background `Thread` + `join(2s)` so it can't deadlock the Main thread. Don't move it back into the Activity (it leaked a wrapped handler chain on every recreation).
2. **Hebrew weekend in category boost**: `AppCategoryProvider.computeCategoryBoost()` treats Friday+Saturday as weekend. Hardcoded; don't surprise yourself when porting.
3. **UsageStats permission prompt fires once per process** ([LauncherActivity.onResume](android/app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt)) — if the user denies, they need to grant from Settings → Apps → NoveLauncher → Usage manually until the next app restart.
4. **OneDrive and Box backups are explicitly disabled** until Microsoft/Box OAuth flows are wired up; `BackupDestination` still lists them (so existing stored settings/exports don't break deserialization) but `backup()` returns a clear error and `BackupSection`'s destination picker filters them out of the UI — don't offer an option that can never succeed.
5. **Legacy credential migration runs on first verify**: PIN/password values stored as unsalted SHA-256 (or plaintext, for `personalZonePin`) are accepted on verify and silently upgraded to PBKDF2 with per-install salt. Same pattern for `SecuritySettings` encryption — plain JSON in DataStore is migrated to AES-256-GCM-via-AndroidKeyStore on first save.
6. **`AppWidgetHost`** is provided to Composables via `LocalAppWidgetHost`. Non-Compose code injects `AppWidgetHost` directly from Hilt.
7. **`NewsCache`** is process-lifetime, 10-min TTL, single-entry.
8. **Timber DebugTree only in debug builds.** For release, plant a custom Tree.
9. **Failed-PIN lockout**: 5 wrong attempts → exponential backoff (30s → 60s → ...). Per-process; cleared on success and on app restart.
10. **App-lock cache invalidates on `ACTION_SCREEN_OFF`** and when `onResume` fires after >30 s of being paused. Apps re-prompt accordingly.
11. **i18n is partial**. Resources exist in `values/`, `values-en/`, `values-ar/`, `values-fr/`, `values-ru/` for ~30 common strings. The bulk of UI text is still inline Hebrew; extraction is straightforward but mechanical.
12. **`appLanguage` applied on Application.onCreate** via `AppCompatDelegate.setApplicationLocales`. Change requires app restart to fully propagate to all activities.
13. **StrictMode is debug-only, detect+log** (`AILauncherApp.installStrictMode`, gated on `BuildConfig.DEBUG`) — never `penaltyDeath`. A violation shows up in Logcat, not as a crash; don't be surprised release builds don't reproduce it.
14. **"What's New" dialog** (`WhatsNewDialog`) compares `OnboardingState.lastSeenVersionCode` to `BuildConfig.VERSION_CODE` in `LauncherActivity` and shows once after an update. `completeOnboarding()` sets `lastSeenVersionCode` to the current version immediately (not 0) so a fresh install doesn't see the dialog right after finishing onboarding. Content lives in `R.array.whats_new_items` — update it per release, not the dialog composable.
15. **Static app shortcuts** (`res/xml/shortcuts.xml`) hardcode `android:targetPackage="com.ailauncher.app"` — resource XML files don't get the `${applicationId}` manifest-merger substitution, so if the applicationId ever changes, update this file by hand too.

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
| Change app-ranking math | [data/AppCategoryProvider.kt](android/app/src/main/java/com/ailauncher/app/data/AppCategoryProvider.kt) (rule-based) + [data/ml/AppPredictionEngine.kt](android/app/src/main/java/com/ailauncher/app/data/ml/AppPredictionEngine.kt) (ML upgrade slot) |
| Add a new settings group | model in [domain/models/Models.kt](android/app/src/main/java/com/ailauncher/app/domain/models/Models.kt), add Preference key + flow + save in [SettingsRepository.kt](android/app/src/main/java/com/ailauncher/app/data/SettingsRepository.kt), expose in [LauncherViewModel.kt](android/app/src/main/java/com/ailauncher/app/ui/LauncherViewModel.kt), wire UI in [SettingsActivity.kt](android/app/src/main/java/com/ailauncher/app/ui/screens/SettingsActivity.kt) |
| Add a launcher page | append a `LauncherPage` constant in [domain/models/Models.kt](android/app/src/main/java/com/ailauncher/app/domain/models/Models.kt), handle in the `when` inside `LauncherRoot` ([LauncherActivity.kt:340-348](android/app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt)) |
| Add a theme preset | append to `ThemePreset.PRESETS` ([Models.kt:16](android/app/src/main/java/com/ailauncher/app/domain/models/Models.kt)) |
| Add a Play-Store-distributable graphic | regenerate via `assets/generate_graphics.py` (PIL-based) — output lands in `assets/graphics/`; see [assets/distribution/](assets/distribution/) for store listing markdown |
| Change icon | drawable XMLs in [android/app/src/main/res/drawable/](android/app/src/main/res/drawable/) + PNGs in [android/app/src/main/res/mipmap-*](android/app/src/main/res/) (themed monochrome in [ic_launcher_monochrome.xml](android/app/src/main/res/drawable/ic_launcher_monochrome.xml)) |
| Add a backup destination | new branch in `BackupManager.backup()` + new entry in `BackupDestination` enum |

## Testing

`junit` + `mockk` + `kotlinx-coroutines-test` + `robolectric` + `androidx.compose.ui:ui-test-junit4` are on the classpath. Run via:

```powershell
# From android/
.\gradlew.bat testDebugUnitTest      # all JVM unit tests
.\gradlew.bat jacocoTestReport       # coverage report → app/build/reports/jacoco/jacocoTestReport/
```

- [AppCategoryProviderTest.kt](android/app/src/test/java/com/ailauncher/app/data/AppCategoryProviderTest.kt) — rule-based ranking scoring math + `autoGroup` filtering.
- [AppPredictionEngineTest.kt](android/app/src/test/java/com/ailauncher/app/data/ml/AppPredictionEngineTest.kt) — `extractFeatures`/`computeFeatureScore` (both `internal`, not `private`, specifically so this test can call them directly) covering recency decay, the hour-of-day affinity window, and the Hebrew-weekend day match.
- [PortableBackupCryptoTest.kt](android/app/src/test/java/com/ailauncher/app/security/PortableBackupCryptoTest.kt) — encrypted-backup round-trip, wrong-password rejection, envelope detection.
- [SettingsRepositoryTest.kt](android/app/src/test/java/com/ailauncher/app/data/SettingsRepositoryTest.kt) — Robolectric, real DataStore against a temp file (`@Config(sdk=[33], application=Application::class)` — plain `Application`, not `AILauncherApp`, to avoid booting Hilt).
- [OnboardingScreenTest.kt](android/app/src/test/java/com/ailauncher/app/ui/screens/OnboardingScreenTest.kt) — first Compose UI test in the project: `createComposeRule()` + Robolectric (`@GraphicsMode(NATIVE)`). Needs `debugImplementation(libs.compose.ui.test.manifest)` in `android/app/build.gradle.kts` — without it, Robolectric can't resolve the synthetic host Activity `createComposeRule()` launches under the hood (`Unable to resolve activity for Intent ... ComponentActivity`). Use `ApplicationProvider.getApplicationContext<Context>().getString(...)` instead of hardcoding locale text when asserting on strings.
- **Not covered, deliberately**: real instrumented (`androidTest`/Espresso) tests and Macrobenchmark/Baseline Profiles both need a device or emulator, which wasn't available while this test suite was built — CI also has no emulator step. Don't add `androidTest` sources without also wiring an emulator into `android.yml`, or they'll never run.
- `android.util.Base64` throws `RuntimeException(... not mocked)` in a plain (non-Robolectric) JVM test — `PortableBackupCrypto` deliberately uses `java.util.Base64` instead (available since minSdk 26) so its tests don't need Robolectric at all.
