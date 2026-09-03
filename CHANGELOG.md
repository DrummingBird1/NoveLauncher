# Changelog

All notable changes to NoveLauncher. Versions follow the app's
`versionName`; each one has a matching [GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases).

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
