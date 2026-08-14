# NoveLauncher — Claude Code Guide

A Hebrew-RTL Android launcher with on-device AI ranking, smart folders, themes, widgets, news feed, backups, and biometric app lock.

## Quick facts

- **App ID:** `com.ailauncher.app` (label "NoveLauncher")
- **Gradle root:** [android/](android/) — the repo root itself is **not** a Gradle project; open `android/` in Android Studio, not the repo root. (An in-progress attempt by a separate session to revert this back to a flat repo-root layout was found and rolled back on 2026-08-08 — it had rewritten this doc's prose and deleted `.gitignore`/`AGENTS.md` without actually moving the directories back, which left the repo in a half-consistent state. If you see doc/reality mismatches like that again, verify the real filesystem with `ls`/`git status` before trusting prose.)
- **Module:** single Android app module `:app` (declared in [settings.gradle.kts](android/settings.gradle.kts))
- **SDK:** `minSdk 26`, `targetSdk 35`, `compileSdk 35`, Java/Kotlin target 17
- **Version:** `versionCode 12`, `versionName "9.3.0"` ([build.gradle.kts](android/app/build.gradle.kts))
- **AGP / Kotlin:** AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Compose BOM 2024.12.01
- **Language:** Kotlin only. i18n covers 7 locales — he/en/ar/fr/ru (v9) plus es/de (v9.2) — 356 keys each (was 353 as of v9.2; v9.3 added `security_root_warning` + `backup_last_run_success`/`backup_last_run_failed`), kept in lockstep (`sed -n 's/.*<string name="\([^"]*\)".*/\1/p' values-XX/strings.xml | sort` across locales should always diff-empty). Enums and `LauncherPage` use `@StringRes Int` displayNameRes (LauncherPage + ThemePreset have a hybrid pattern — built-in entries set the res id, user-created entries keep the typed String). The four NewsSource Hebrew brand names (Walla/Haaretz/Kan/Calcalist) are kept verbatim because they are the outlets' actual names. `AppearanceSettings.appLanguage` has a real picker UI (Settings → Appearance → language chips) — see Conventions below for why it also calls `.recreate()`.
- **Git:** Tracked in git, origin = [github.com/DrummingBird1/NoveLauncher](https://github.com/DrummingBird1/NoveLauncher). `main` is the only branch. GitHub Actions ([.github/workflows/android.yml](.github/workflows/android.yml)) runs three parallel jobs on every push/PR to main (v9.3 — was one sequential job): `build` (assembleDebug/Release, lint, ktlint, APK size report), `test` (unit tests, Jacoco, `verifyPaparazziDebug` screenshot tests), and `instrumented-tests` (emulator-based `connectedDebugAndroidTest`, `continue-on-error: true` since it's new and unproven). Lint/ktlint/instrumented-tests are all non-blocking. [.github/workflows/release.yml](.github/workflows/release.yml) (v9.3) builds release APK+AAB and opens a **draft** GitHub Release on any `vX.Y.Z` tag push — signed if the keystore secrets below are present, an explicit CI warning if not. [.github/dependabot.yml](.github/dependabot.yml) opens weekly PRs for Gradle + Actions dependency bumps, but ignores major-version bumps on `androidx.biometric`/`androidx.security:security-crypto` (those need a human reading the changelog first). Update flow: `git add . && git commit -m "…" && git push`.
- **Release signing** is opt-in via env vars read in [android/app/build.gradle.kts](android/app/build.gradle.kts) (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) — absent any of them, `assembleRelease`/`bundleRelease` stay unsigned exactly like before. To actually sign a release: generate a keystore yourself (`keytool -genkeypair -v -keystore release.keystore -alias novelauncher -keyalg RSA -keysize 2048 -validity 10000`), keep it out of git, and either export those four env vars locally or add `RELEASE_KEYSTORE_BASE64` (base64 of the keystore file) + the three password/alias secrets to the GitHub repo's Actions secrets — CI decodes and signs automatically once present. No release keystore is generated or stored by this repo. **Debug builds are different**: they always sign with a committed, throwaway keystore, [android/app/debug.keystore](android/app/debug.keystore) (fixed v9.3, see Pitfall #21) — don't regenerate it.
- **Crash reporting** (Sentry) is opt-in via a `SENTRY_DSN` build-time env var (v9.3) — same pattern as release signing. No DSN is set anywhere in this repo, so `AILauncherApp.initSentry()` is a no-op today.
- **Dependencies:** managed via Gradle version catalog ([gradle/libs.versions.toml](android/gradle/libs.versions.toml)), including a `[bundles]` section (v9.3) for groups of libraries that always land together (`libs.bundles.compose`). Don't hardcode versions in build.gradle.kts — bump in the catalog instead. Glance and Material 2 were dropped in v9; re-add via the catalog if/when needed.
- **Repo root layout (v9.1):** exactly two folders at the true repo root. [android/](android/) is the entire Gradle/Android Studio project (open **this** folder in Android Studio, not the repo root) — root build files, `gradle/`, and the single `:app` module live inside it. [assets/](assets/) holds everything else non-code: design source (`generate_graphics.py`, `graphics/`, `screenshots/`), [assets/distribution/](assets/distribution/) (Play Store submission text + [assets/distribution/ALTERNATIVE_STORES.md](assets/distribution/ALTERNATIVE_STORES.md), a launch plan for stores other than Google Play), and [assets/docs/adr/](assets/docs/adr/) (v9.3 — Architecture Decision Records; lives under `assets/`, not the repo root, to keep the two-folder rule). `.github/` stays at the true repo root regardless (GitHub only reads workflows/dependabot config from `<repo-root>/.github/`), as do `CLAUDE.md`/`AGENTS.md` (Claude Code / Codex only auto-load project instructions from the root they're opened in).

## Build & run

```powershell
# From android/ (open THIS folder in Android Studio, not the repo root)
.\gradlew.bat assembleDebug        # APK
.\gradlew.bat bundleRelease        # AAB for Play Store
.\gradlew.bat installDebug         # install on connected device
.\gradlew.bat lint                 # AGP lint
.\gradlew.bat test                 # unit tests (includes Paparazzi screenshot tests)
.\gradlew.bat verifyPaparazziDebug # screenshot tests only, against goldens in app/src/test/snapshots/
.\gradlew.bat recordPaparazziDebug # regenerate those goldens after an intentional visual change
```

Open `android/` in Android Studio Ladybug+ (not the repo root — the repo root is no longer a Gradle project). The release build runs R8 in **full mode** (v9.3, `android.enableR8.fullMode=true` in [gradle.properties](android/gradle.properties) — stricter about keep rules than the default mode) — ProGuard keep rules for serialization models live in [proguard-rules.pro](android/app/proguard-rules.pro). Gradle Configuration Cache is also on (v9.3) — a future config-cache-incompatible plugin bump fails loudly and clearly at build time rather than silently degrading.

## Architecture

Layered, Hilt-driven, single-Activity Compose UI.

```
ui/                            ← Compose screens + ViewModel + Activity entry
  LauncherActivity.kt          ← FragmentActivity (needed for BiometricPrompt). Single host.
  LauncherViewModel.kt         ← @HiltViewModel, central state via StateFlow
  screens/                     ← HomeScreen, AppsScreen, NewsScreen, PersonalZoneScreen,
                                 StatisticsScreen, GlobalSearchScreen, OnboardingScreen,
                                 SettingsActivity.kt (separate Activity, sections split into
                                 AppearanceSettings.kt/BackupSettings.kt/SecuritySettings.kt/etc.)
  components/                  ← LauncherWidgetProvider (AppWidget receiver),
                                 LockLauncherTileService (Quick Settings tile)
  theme/                       ← Theme.kt, font + color mapping for ThemePreset

domain/
  models/                      ← settings + UI state types, split across 5 files (CoreModels.kt,
                                 AppearanceModels.kt, PlatformModels.kt, etc. — not one Models.kt)
  models/AppCategory.kt        ← 11-category enum w/ Play Store mapping
  usecases/GetRankedAppsUseCase.kt  ← combines apps + usage + ml + notification badges
  repository/                  ← domain-layer interfaces for SettingsRepository and
                                 InstalledAppsRepository (impls in data/) — di/AppBindsModule.kt
                                 binds them via @Binds alongside AppModule's @Provides

data/
  SettingsRepository.kt        ← DataStore-backed, serializes everything as JSON strings
  InstalledAppsRepository.kt   ← PackageManager query, cached + invalidated by package broadcasts
  UsageStatsRepository.kt      ← UsageStatsManager events → snapshots
  AppCategoryProvider.kt       ← heuristic ranker (recency + freq + time-of-day + category)
  IconCache.kt                 ← memory LRU + disk-backed warm cache for launcher icons (v9.3)
  LauncherNotificationListener.kt  ← NotificationListenerService → unread badges
  db/LauncherDatabase.kt       ← Room: 5 entities + 2 DAOs, schema version 2 (v9.3 — indices;
                                 see db/Migrations.kt's MIGRATION_1_2)
  backup/BackupManager.kt      ← local + Google Drive + OneDrive(disabled) + Box(disabled) + NAS
  backup/ScheduledBackupWorker.kt  ← WorkManager periodic backup, records success/failure +
                                 posts a failure notification
  iconpack/IconPackManager.kt  ← Nova/ADW/Lawnchair compatible appfilter.xml reader
  ml/AppPredictionEngine.kt    ← on-device prediction, designed to swap in TFLite later
  api/WeatherService.kt        ← Open-Meteo, direct HttpURLConnection + kotlinx.serialization

security/
  AppLockManager.kt            ← PIN/password/pattern/biometric per-app + launcher-wide lock
  BiometricHelper.kt           ← BiometricPrompt wrapper
  RootDetection.kt             ← heuristic, deterrent-only root/tamper signal (v9.3)

di/AppModule.kt, AppBindsModule.kt  ← Hilt @Provides / @Binds for everything above
AILauncherApp.kt               ← @HiltAndroidApp + WorkManager Configuration.Provider
                                 + crash handler + optional Sentry init (v9.3)
```

### Key flows

- **Home rendering pipeline**: `LauncherViewModel.refresh()` → `GetRankedAppsUseCase.execute()` → reads installed apps + usage events + notification badges → `AppCategoryProvider.rankApps()` (recency × 0.35 + freq × 0.30 + time-of-day × 0.25 + category boost × 0.10) → `autoGroup()` for smart folders → `IconCache.preloadFromDisk()` warms the icon cache from disk (v9.3, cold-start optimization) before the state update → emits `AppListState`. Filtered through `hiddenApps`/`privateFolderPackages` before reaching UI.
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
11. **i18n is complete, not partial** — see "Language" in Quick facts above; this note used to say otherwise and predated the v9 i18n pass. Don't reintroduce inline literals.
12. **`appLanguage` applied on Application.onCreate** via `AppCompatDelegate.setApplicationLocales`, *and* the in-app picker (`AppearanceSettings.kt`) calls `.recreate()` immediately after changing it. Neither `LauncherActivity` nor `SettingsActivity` extends `AppCompatActivity` (they're `FragmentActivity`/`ComponentActivity`), so AppCompat's usual "recreates AppCompatActivities for you on locale change" convenience doesn't apply here — don't remove the manual `.recreate()` call assuming AppCompat has it covered.
13. **StrictMode is debug-only, detect+log** (`AILauncherApp.installStrictMode`, gated on `BuildConfig.DEBUG`) — never `penaltyDeath`. A violation shows up in Logcat, not as a crash; don't be surprised release builds don't reproduce it.
14. **"What's New" dialog** (`WhatsNewDialog`) compares `OnboardingState.lastSeenVersionCode` to `BuildConfig.VERSION_CODE` in `LauncherActivity` and shows once after an update. `completeOnboarding()` sets `lastSeenVersionCode` to the current version immediately (not 0) so a fresh install doesn't see the dialog right after finishing onboarding. Content lives in `R.array.whats_new_items` — update it per release, not the dialog composable.
15. **Static app shortcuts** (`res/xml/shortcuts.xml`) hardcode `android:targetPackage="com.ailauncher.app"` — resource XML files don't get the `${applicationId}` manifest-merger substitution, so if the applicationId ever changes, update this file by hand too.
16. **`SettingsActivity`'s deep-link extra is generic, not hardcoded per page**: `EXTRA_SHORTCUT_PAGE` is parsed via `SettingsPage.valueOf(...)` (falls back to `MAIN` on a bad/missing value), not a `when` with one branch per page. Both the static shortcuts XML and Global Search's settings results (`SEARCHABLE_SETTINGS_PAGES` in `GlobalSearchScreen.kt`) pass the enum's own `.name` — adding a new deep-linkable page needs zero changes to `SettingsActivity.onCreate`.
17. **`GetRankedAppsUseCase` computes zero notification counts, not filtered-at-render counts**, when badges are off (`AppearanceSettings.showNotificationBadges`) or snoozed (`badgeSnoozedUntil` in the future) — cheaper than computing real counts and hiding them in the UI, but means `RankedApp.notificationCount` is genuinely `0` in that state, not just visually suppressed. Don't add a second badge-gating check downstream expecting non-zero counts to still be there.
18. **`LockLauncherTileService`'s "lock now" action clears the unlock grace period, it does not toggle `launcherLockMethod`** (`AppLockManager.revokeLauncherUnlock()`, not `saveSecurity(sec.copy(launcherLockMethod = NONE))`). Flipping the method to `NONE` would silently disable whatever credential the user configured; expiring the unlock window just forces re-authentication next resume, which is reversible and doesn't touch stored credentials.
19. **`HttpURLConnection` can't do WebDAV `MKCOL` without a reflection workaround** — `setRequestMethod` allow-lists standard HTTP verbs and throws `ProtocolException` on anything else. `BackupManager.tryCreateNasDirectory()` falls back to setting the private `method` field via reflection when that happens, and is deliberately best-effort (wrapped in try/catch) — if reflection is blocked on some Android version, NAS backup just behaves exactly like it did before this existed (PUT fails with the original error) rather than crashing.
20. **`AILauncherApp.BACKUP_CHANNEL_ID` is the first `NotificationChannel` this app has ever created** — `ScheduledBackupWorker` is the only current poster. `NotificationManagerCompat.notify()` is still wrapped in `catch (_: SecurityException)` so a denied `POST_NOTIFICATIONS` request silently no-ops instead of crashing the worker — but as of v9.3 the permission is actually requested (see Pitfall #24 below), so this is now the "user said no" path, not the "we never asked" path it used to be.
21. **Debug builds sign with a committed keystore, `android/app/debug.keystore`** (fixed 2026-08-14) — before this, debug builds used AGP's implicit default, which auto-generates `~/.android/debug.keystore` with *random* key material the first time it's missing on a machine. Every GitHub Actions runner is a fresh VM, so every CI run got its own throwaway signature; installing a newer CI-built debug APK over an older one failed with "App not installed as package conflicts with an existing package" (signature mismatch, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`) — not a Play Protect/unknown-sources block, so disabling those did nothing. Fix: `debug.keystore` is explicitly exempted from the `*.keystore` gitignore rule and wired into `signingConfigs.debug` in [build.gradle.kts](android/app/build.gradle.kts) with the same alias/password AGP itself defaults to (`androiddebugkey`/`android`) — it's a throwaway debug-only key, safe to share. Anyone with the app already installed under an old random-keyed debug build needs to uninstall it **once**; every debug build from here on (local or CI) shares this one signature and updates cleanly. Don't regenerate this file casually — doing so breaks the same way it was just fixed.
22. **`IconCache`'s disk layer is write-only from the hot path** (v9.3) — `getOrLoad()` is called synchronously from Composable bodies (HomeScreen/AppsScreen), so it never touches disk itself (that would trip `StrictMode.detectDiskReads()`/`detectDiskWrites()`, Pitfall #13, and block the Main thread for real on a release build). Instead a cache miss fires an async, best-effort disk write via `IconCache.ioScope`, and `preloadFromDisk(keys)` (suspend) is the *only* read path — called once from `LauncherViewModel.refresh()`, before the new app list reaches composition. Don't add a second call site that reads disk synchronously from `getOrLoad()`.
23. **`RootDetection.isLikelyRooted()` is a heuristic, deterrent-only signal** (v9.3) — su-binary path checks + the `test-keys` build tag, both trivially defeated by root-hiding tools (Magisk Hide/Zygisk). It's surfaced as an informational banner in Security settings and is **never** used to gate, block, or weaken any security feature — consistent with the "app lock is a deterrent, not a sandbox" model above. Don't wire it into any enforcement path.
24. **`POST_NOTIFICATIONS` is now requested at runtime** (v9.3, `LauncherActivity.onCreate`, API 33+ only) — fixes the gap Pitfall #20 used to describe. The `ActivityResultContracts.RequestPermission()` callback is intentionally a no-op either way; a denial just means backup-failure alerts stay silent (same behavior as before this existed), there's nothing else to react to.
25. **`LauncherDatabase` is at schema version 2** (v9.3 — indices added on `daily_stats(date)`, `daily_stats(packageName)`, `hourly_usage(packageName)`) via a real (non-destructive) `Migration` in `db/Migrations.kt`, not `fallbackToDestructiveMigration()` — the existing `fallbackToDestructiveMigrationOnDowngrade()` only ever covered downgrades, so a version bump without a matching `Migration` would have crashed on upgrade for existing installs. `exportSchema = true` + `room.schemaLocation` (ksp block in `app/build.gradle.kts`) now export schema JSON to `android/app/schemas/`, which is committed to git and which `LauncherDatabaseMigrationTest` validates the migration against. **Any future schema change needs both a new `Migration` and a version bump** — regenerate schema JSON with `./gradlew kspDebugKotlin` before writing the migration test. The schema JSON is also shipped as a `debug`-build-only asset (`sourceSets["debug"].assets`) — that's how `MigrationTestHelper`/Robolectric find it; it is not present in release builds.
26. **Sentry init order matters**: `AILauncherApp.initSentry()` runs *before* `installCrashHandler()` in `onCreate()` — Sentry installs its own uncaught-exception handler, so initializing it first means this app's own handler (which does its own crash bookkeeping via `SettingsRepository.recordCrash()`) ends up chaining *through* Sentry's handler rather than the other way around. Both still see every crash either way, since each handler chains to whatever was previously registered — but keep this order if either init function ever moves.
27. **`AppBindsModule` coexists with `AppModule`'s `@Provides`, it doesn't replace them** — `SettingsRepository`/`InstalledAppsRepository` are still *constructed* via `AppModule`'s `@Provides` functions (which know how to build the concrete `data.*` classes); `AppBindsModule`'s `@Binds` just additionally exposes those same instances under their `domain.repository.*` interface types, for callers/tests that want to depend on the abstraction. Removing `AppModule`'s `@Provides` for either class would break `AppBindsModule`'s `@Binds`, not the other way around.

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
| Add a new settings group | model in [domain/models/](android/app/src/main/java/com/ailauncher/app/domain/models/), add Preference key + flow + save in [SettingsRepository.kt](android/app/src/main/java/com/ailauncher/app/data/SettingsRepository.kt), expose in [LauncherViewModel.kt](android/app/src/main/java/com/ailauncher/app/ui/LauncherViewModel.kt), wire UI in [SettingsActivity.kt](android/app/src/main/java/com/ailauncher/app/ui/screens/SettingsActivity.kt) |
| Add a launcher page | append a `LauncherPage` constant in domain/models, handle in the `when` inside `LauncherRoot` in [LauncherActivity.kt](android/app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt) |
| Add a theme preset | append to `ThemePreset.PRESETS` |
| Add a Play-Store-distributable graphic | regenerate via `assets/generate_graphics.py` (PIL-based) — output lands in `assets/graphics/`; see [assets/distribution/](assets/distribution/) for store listing markdown |
| Change icon | drawable XMLs in [android/app/src/main/res/drawable/](android/app/src/main/res/drawable/) + PNGs in [android/app/src/main/res/mipmap-*](android/app/src/main/res/) (themed monochrome in [ic_launcher_monochrome.xml](android/app/src/main/res/drawable/ic_launcher_monochrome.xml)) |
| Add a backup destination | new branch in `BackupManager.backup()` + new entry in `BackupDestination` enum |
| Add a deep-linkable settings page from Global Search or a shortcut | append to `SEARCHABLE_SETTINGS_PAGES` in [GlobalSearchScreen.kt](android/app/src/main/java/com/ailauncher/app/ui/screens/GlobalSearchScreen.kt) and/or `res/xml/shortcuts.xml` — both just pass a `SettingsPage.name`, no `SettingsActivity` changes needed |
| Add a language | new `values-XX/strings.xml` with all 356 keys + add `"XX" to "Native name"` in the `languages` list in [AppearanceSettings.kt](android/app/src/main/java/com/ailauncher/app/ui/screens/AppearanceSettings.kt) |
| Change the Room schema | add fields/entities in [data/db/LauncherDatabase.kt](android/app/src/main/java/com/ailauncher/app/data/db/LauncherDatabase.kt), bump `version`, add a `Migration` in [data/db/Migrations.kt](android/app/src/main/java/com/ailauncher/app/data/db/Migrations.kt), regenerate schema JSON with `./gradlew kspDebugKotlin` (see Pitfall #25), extend `LauncherDatabaseMigrationTest` |
| Cut a GitHub Release | `git tag vX.Y.Z && git push origin vX.Y.Z` — [.github/workflows/release.yml](.github/workflows/release.yml) builds + opens a draft release automatically |

## Testing

`junit` + `mockk` + `kotlinx-coroutines-test` + `robolectric` + `androidx.compose.ui:ui-test-junit4` + Paparazzi (screenshot testing) + MockWebServer + `androidx.room:room-testing` are all on the classpath. Run via:

```powershell
# From android/
.\gradlew.bat testDebugUnitTest      # all JVM unit tests, including Paparazzi
.\gradlew.bat verifyPaparazziDebug   # screenshot tests only, against goldens in app/src/test/snapshots/
.\gradlew.bat jacocoTestReport       # coverage report → app/build/reports/jacoco/jacocoTestReport/
```

- [AppCategoryProviderTest.kt](android/app/src/test/java/com/ailauncher/app/data/AppCategoryProviderTest.kt) — rule-based ranking scoring math + `autoGroup` filtering.
- [AppPredictionEngineTest.kt](android/app/src/test/java/com/ailauncher/app/data/ml/AppPredictionEngineTest.kt) — `extractFeatures`/`computeFeatureScore` (both `internal`, not `private`, specifically so this test can call them directly) covering recency decay, the hour-of-day affinity window, and the Hebrew-weekend day match.
- [GetRankedAppsUseCaseTest.kt](android/app/src/test/java/com/ailauncher/app/domain/usecases/GetRankedAppsUseCaseTest.kt) (v9.3) — badge suppression when off/snoozed vs. passthrough, and that usage snapshots are skipped entirely without permission. Mocks every collaborator with mockk (plain, not `-android`; final Kotlin classes mock fine on the JVM).
- [PortableBackupCryptoTest.kt](android/app/src/test/java/com/ailauncher/app/security/PortableBackupCryptoTest.kt) — encrypted-backup round-trip, wrong-password rejection, envelope detection.
- [SettingsRepositoryTest.kt](android/app/src/test/java/com/ailauncher/app/data/SettingsRepositoryTest.kt) — Robolectric, real DataStore against a temp file (`@Config(sdk=[33], application=Application::class)` — plain `Application`, not `AILauncherApp`, to avoid booting Hilt).
- [IconCacheTest.kt](android/app/src/test/java/com/ailauncher/app/data/IconCacheTest.kt) (v9.3) — Robolectric, real disk I/O round-trip (write → simulated process restart via a fresh `IconCache` instance → `preloadFromDisk` → read). Swaps `IconCache.ioScope` for an `UnconfinedTestDispatcher`-backed scope so the async disk write completes before the test's next assertion instead of racing a real background thread.
- [WeatherServiceTest.kt](android/app/src/test/java/com/ailauncher/app/data/api/WeatherServiceTest.kt) (v9.3) — MockWebServer contract test against Open-Meteo's real response shape. `WeatherService.baseUrl` is `internal var`, test-only override point (production/Hilt never sets it); always passes explicit lat/lon so `getCurrentLocation()`'s `Context`/`LocationManager` calls are never reached, so a plain `mockk(relaxed = true)` Context is enough — no Robolectric needed.
- [LauncherDatabaseMigrationTest.kt](android/app/src/test/java/com/ailauncher/app/data/db/LauncherDatabaseMigrationTest.kt) (v9.3) — `MigrationTestHelper` against the real exported schema JSON (see Pitfall #25), not a hand-maintained copy. Robolectric-based (not `androidTest`) — `MigrationTestHelper`/`InstrumentationRegistry` work fine under Robolectric.
- [HomeAppItemSnapshotTest.kt](android/app/src/test/java/com/ailauncher/app/ui/screens/HomeAppItemSnapshotTest.kt) (v9.3) — Paparazzi screenshot tests (JVM-only, renders via layoutlib, no device needed) for icon-shape rendering and the notification badge. Goldens live in `app/src/test/snapshots/`; `recordPaparazziDebug` to regenerate after an intentional visual change, `verifyPaparazziDebug` (wired into CI's `test` job) to catch accidental ones.
- [OnboardingScreenTest.kt](android/app/src/test/java/com/ailauncher/app/ui/screens/OnboardingScreenTest.kt) — first Compose *interaction* UI test in the project: `createComposeRule()` + Robolectric (`@GraphicsMode(NATIVE)`). Needs `debugImplementation(libs.compose.ui.test.manifest)` in `android/app/build.gradle.kts` — without it, Robolectric can't resolve the synthetic host Activity `createComposeRule()` launches under the hood (`Unable to resolve activity for Intent ... ComponentActivity`). Use `ApplicationProvider.getApplicationContext<Context>().getString(...)` instead of hardcoding locale text when asserting on strings.
- [ApplicationIdTest.kt](android/app/src/androidTest/java/com/ailauncher/app/ApplicationIdTest.kt) (v9.3) — the project's first real instrumented (`androidTest`) test, deliberately trivial (just checks the target package name). Its job is validating the emulator CI pipeline itself (the `instrumented-tests` job in `android.yml`, `continue-on-error: true` since it's new and unproven), not real app behavior. `LauncherActivity` is `@AndroidEntryPoint` + `singleTask`/`HOME`, so exercising it for real would need a `HiltTestApplication` test runner + `@HiltAndroidRule` — a separate, larger addition, deliberately not done yet.
- `android.util.Base64` throws `RuntimeException(... not mocked)` in a plain (non-Robolectric) JVM test — `PortableBackupCrypto` deliberately uses `java.util.Base64` instead (available since minSdk 26) so its tests don't need Robolectric at all.
