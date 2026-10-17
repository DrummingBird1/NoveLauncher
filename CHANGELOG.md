# Changelog

All notable changes to NoveLauncher. Versions follow the app's
`versionName`; each one has a matching [GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases).

---

## [10.0.0] — Unreleased
*On `main`, not released yet — will ship as versionCode 13. Profiles, shortcuts, wallpapers, gestures, smart search and the Jewish calendar.*

### Added
- **Work profile and Private Space.** Apps now come from every profile on the
  device. Work apps carry their badge, the Apps page filters All / Personal /
  Work, and work apps can be paused and turned back on. Android 15's Private
  Space gets its own section with Lock / Unlock; its apps never appear in
  suggestions or smart folders and are searchable only while the space is
  unlocked. Cloned apps are labelled as such.
- **App shortcuts.** Long-press an app to see its shortcuts (static, dynamic
  and pinned) and open them in the right profile.
- **Wallpapers.** Your own photos (through Android's photo picker) or
  built-in gradients, shown as one wallpaper everywhere, per page, by hour,
  by weekday — or your system wallpaper, static or live, behind the launcher.
  Brightness, contrast, saturation, warmth, blur, vignette and opacity per
  wallpaper, and an optional dim while scrolling.
- **Gestures.** Swipe up for apps, swipe down for the notification shade,
  double-tap the home screen to turn the screen off (Settings → Gestures).
  Double-tap uses an optional accessibility service that performs only two
  actions — screen off and open notifications — and cannot read the screen;
  the app explains this before sending you to Accessibility settings.
- **Smart search.** Hebrew ↔ English matching ("וואטסאפ" finds WhatsApp,
  "walla" finds וואלה), typo tolerance, initials ("gm" → Google Maps), and
  niqqud / final letters ignored — on the Apps page and in global search.
  Instant answers: a calculator ("15% of 80", "2^10") and unit conversion
  for length, weight, volume, speed and temperature, in English and Hebrew
  ("5 ק"ג לפאונד"), with a copy button.
- **Jewish calendar and Shabbat mode.** Hebrew date, holidays on the Israeli
  or diaspora schedule (Israel's national days with their official
  date-shift rules), and candle-lighting and Havdalah times calculated on the
  phone from your approximate location, or Tel Aviv. Shabbat mode: from
  candle lighting until nightfall, the home screen hides social,
  entertainment, game and shopping suggestions and notification badges and
  greets you with Shabbat Shalom. Nothing is blocked.
- **Suggestions that learn.** "Recommended now" remembers which suggestions
  were shown and which you actually tapped (fading over time) and ranks
  accordingly; connected headphones nudge media apps up. Reset it from the
  Statistics page.
- **Widgets, end to end.** Adding a widget runs its configuration screen,
  cancelled or removed widgets release their IDs, providers are told their
  real size, and widget height is resizable.
- **Google Drive connect / disconnect** in Settings → Backup, showing which
  account is connected. Only the `drive.file` scope (files the app created)
  and `email` are requested.
- **Optional update check** in Settings → About: off by default; when on, it
  asks GitHub's releases API at most once a day (or when you tap "Check now")
  and links to the release. Nothing is downloaded or installed by the app.

### Fixed
- **Google Drive backup could never connect.** The sign-in code existed but
  nothing called it, so choosing Google Drive always failed — and Google has
  since removed the GoogleSignIn API it used. Drive now connects through
  Google Identity Services from Settings → Backup.
- **The Statistics page was always empty.** It read a table that nothing ever
  wrote. It now reads Android's usage statistics directly, by day, week or
  month.
- **Icon packs couldn't be applied.** They were listed in Settings but could
  not be selected. Now the chosen pack is applied everywhere; apps it doesn't
  cover keep their own icon, and work / cloned apps keep the system badge.
- **About linked to a dead domain.** The privacy policy and terms buttons
  pointed at `novelauncher.app`, which doesn't exist. About now links to the
  live website, privacy policy, terms and source code.
- **Onboarding flashed on every cold start**, in the default theme, before
  settings had loaded. The splash screen now stays up until they have.
- **Unreadable status bar.** A dark launcher theme on a phone in light mode got
  dark status- and navigation-bar icons. Their colour now follows the
  launcher's theme.
- **Back didn't close search.** System Back now closes the search overlay.
- Swipe up to open Apps silently did nothing on Android 12+ — the grid's
  stretch overscroll swallowed the gesture.
- Hidden and private-folder apps showed up in search results.
- An app with two launcher activities, or installed in two profiles, could
  crash the app grid.
- Wallpaper settings had no visible effect, and the gesture switches (in
  Settings since v5) did nothing.

### Changed (technical)
- **targetSdk 36** — required by Google Play for new apps and updates since
  2026-08-31 — and compileSdk 37.
- Toolchain: AGP 8.7.3 → 9.4.1 with built-in Kotlin, Gradle 9.8.1,
  Kotlin 2.4.21, KSP 2.3.12. Gradle now runs on **JDK 21** (Paparazzi 2.0
  requires it, CI included); the app still compiles to Java 17 bytecode.
- Dependencies: Hilt 2.60.1, Room 2.8.5, play-services-auth 22,
  Robolectric 4.17, MockK 1.14, OkHttp 5, Compose BOM 2026.09,
  Lifecycle 2.11, Navigation 2.10 and current GitHub Actions.
- Sentry: `sentry-android-core` instead of the umbrella artifact. The release
  APK shrinks from 7.96 MB to 3.58 MB, with no NDK libraries left (so no
  16 KB page-size risk). Crash reporting stays off — no DSN is configured.
- Google Drive authorization moved to Google Identity Services'
  `AuthorizationClient` (`DriveAuth`). It needs an Android OAuth client per
  signing key in Google Cloud — see
  [GOOGLE_DRIVE_SETUP.md](assets/distribution/GOOGLE_DRIVE_SETUP.md).
- App list, launching and shortcuts through `LauncherApps` instead of
  `PackageManager`; entries are keyed by activity + profile.
- `LauncherActivity` split into `LauncherRoot`, `UnlockDialog`, `AppLaunch`,
  `WidgetHostController` and the gesture and wallpaper layers.
- Room schema v3 (`suggestion_feedback`) with a non-destructive migration.
  Migrations are written once as SQL and run through both Room APIs; the
  migration test uses the driver-based helper (the legacy one breaks on
  Windows paths) and covers 1→2, 2→3 and 1→3.
- New permissions: `ACCESS_HIDDEN_PROFILES` (Private Space), `EXPAND_STATUS_BAR`
  (normal; swipe-down fallback) and the optional `LockScreenAccessibilityService`
  — see [PLAY_POLICY_NOTES.md](assets/distribution/PLAY_POLICY_NOTES.md) for the
  Play Console declarations.
- Tests: 94 JVM tests, adding search matching, instant answers, feedback
  scoring, wallpaper rules, the update checker, sunset maths and the Jewish
  calendar. A Hilt instrumented test drives the real `LauncherActivity`
  (home, app-lock flow); it compiles but hasn't run on a device yet. Jacoco
  now reads AGP 9's class output (it had been reporting 0% against a stale
  directory); Paparazzi 2.0 goldens re-recorded.
- New `:baselineprofile` module — Baseline Profile generator and a startup
  macrobenchmark. The profile itself hasn't been generated yet.

---

## v9.3.0 — Infrastructure, testing and performance overhaul
*Released 2026-08-14 · versionCode 12*

### Fixed
- **Debug builds could not be installed over each other.** Every CI run
  generated its own random debug signing key, so a newer debug APK failed
  with "App not installed as package conflicts with an existing package".
  Debug builds now sign with a committed, throwaway keystore, so local and CI
  builds always share one signature.
- **The app crashed on every launch** once crash reporting was added — the
  Sentry SDK auto-initialises from a ContentProvider before the app's own
  code runs, and threw because no DSN is configured. Auto-init is now
  disabled; initialisation is fully controlled by the app.

### Added
- Disk-backed icon cache that warms the in-memory cache on cold start.
- Optional crash reporting (Sentry), opt-in via a `SENTRY_DSN` build-time
  variable — inert and sends nothing when unset.
- Root/tamper detection surfaced as an informational banner in Security
  settings (deterrent only — it never gates or weakens any feature).
- Runtime `POST_NOTIFICATIONS` request, so scheduled-backup failure alerts
  can actually appear.
- Last backup status (time + success/failure) in Settings → Backup.
- Room schema v2 with indices on the statistics tables, via a real
  non-destructive migration.
- Screenshot tests (Paparazzi), a MockWebServer contract test for the weather
  API, a Room migration test, an icon-cache disk round-trip test and use-case
  tests for notification-badge suppression.
- Release-on-tag workflow that builds and drafts a GitHub Release with the
  APK and AAB attached.

### Changed
- CI split into parallel build / test / instrumented-test jobs.
- R8 full mode and Gradle configuration cache enabled.
- Dependabot now requires manual review for major bumps of the security
  libraries.

---

## v9.2.0 — Language picker, custom RSS, quick-settings tile
*Released 2026-08-08 · versionCode 11*

### Added
- **In-app language picker** — the app already supported multiple languages
  internally, but nothing let you choose one. Now 7 are selectable:
  Hebrew, English, Arabic, French, Russian, and new Spanish and German.
- **Custom RSS news sources** — add your own feeds alongside the built-ins.
- **Quick Settings tile** to lock the launcher instantly.
- **Notification badge snooze** — mute badges for an hour or until tomorrow.
- Notification badges are now actually rendered on home-screen icons.
- Settings are now searchable from Global Search.
- Automatic pruning of old local backups, keeping the newest N.
- NAS/WebDAV backups create the target directory automatically.
- A notification when a scheduled backup fails, instead of failing silently.
- News falls back to the last cached results when the network is unavailable.

---

## v9.1.0 — Accessibility, encrypted backups, dynamic colour
*Released 2026-08-07 · versionCode 10*

### Added
- Password-protected portable backups (PBKDF2-derived AES-256-GCM), so an
  exported file can be restored on a different device.
- Material You dynamic colour on Android 12+.
- Static app shortcuts (long-press the launcher icon).
- Reduce-motion support across animated screens.
- An explainer in Security settings about what app lock does and does not
  protect against.

### Changed
- CI hardening: lint, ktlint and coverage reports uploaded on every run.

---

## v9.0.0 — Full internationalisation, hardened crypto, wired-up features
*Released 2026-06-26 · versionCode 9*

### Added
- Complete internationalisation — every UI string moved to resources across
  Hebrew, English, Arabic, French and Russian.
- PBKDF2-HMAC-SHA256 credential hashing with a per-install salt, and
  AES-256-GCM encryption of security settings at rest via the Android Keystore.
- Failed-attempt lockout with exponential backoff, persisted so a force-stop
  cannot reset it.
- A process-wide crash handler and an in-memory icon cache.
- A support button for the project.

### Fixed
- Several features that existed in code but were never reachable are now
  wired up: weather under the clock, icon packs, usage statistics, global
  search, the analog clock face and the home dock.

---

## v8.0.0 — Logo redesign, splash screen, privacy policy
*Released 2026-06-03 · versionCode 8*

### Added
- A new logo system: full-colour icon, monochrome variant and a horizontal
  lockup.
- A splash screen on cold start.
- Themed icon support on Android 13+.
- Privacy policy and terms of service.

### Fixed
- An app-lock bypass — every launch path now routes through the lock check.
- Local backups now use `MediaStore.Downloads` on Android 10+, where the
  legacy path silently failed.
