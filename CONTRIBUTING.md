# Contributing to NoveLauncher

Thanks for taking the time. Bug reports, translation fixes and pull requests
are all welcome.

## Getting set up

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Open the **`android/`** folder in Android Studio (Ladybug or newer). The
repository root is intentionally not a Gradle project — it holds only
`android/` (the app) and `assets/` (everything else). See
[ADR-0001](assets/docs/adr/0001-android-assets-repo-split.md) for why.

Requirements: JDK 17, Android SDK 35.

## Before you open a pull request

```bash
./gradlew assembleDebug          # must pass
./gradlew testDebugUnitTest      # must pass
./gradlew verifyPaparazziDebug   # screenshot tests must pass
./gradlew lint ktlintCheck       # advisory — findings won't block CI
```

If you intentionally change something visual, regenerate the screenshot
goldens with `./gradlew recordPaparazziDebug` and commit the updated PNGs
alongside the code change.

## Conventions this codebase follows

Most of these exist because breaking them previously caused a real bug — the
reasoning is documented in the "Pitfalls" section of [CLAUDE.md](CLAUDE.md).

- **No hardcoded UI strings.** Every user-visible string is a resource, and
  all 7 locales (`values/`, `values-en`, `-ar`, `-fr`, `-ru`, `-es`, `-de`)
  must stay key-for-key identical. Verify with:
  ```bash
  sed -n 's/.*<string name="\([^"]*\)".*/\1/p' values-XX/strings.xml | sort
  ```
  diffed across locales — it should come back empty.
- **RTL-aware layout.** Hebrew and Arabic are first-class; use
  start/end padding, never left/right.
- **Hilt for everything.** No static singletons, no manual dependency
  construction.
- **Don't hardcode dependency versions** — bump them in
  `android/gradle/libs.versions.toml`.
- **Room schema changes need a migration** and a version bump, plus
  regenerated schema JSON (`./gradlew kspDebugKotlin`) and a migration test.
  Never rely on destructive migration for upgrades.
- **Don't regenerate `android/app/debug.keystore`.** It is committed on
  purpose so every machine and CI run produces installable debug builds with
  a matching signature.

## Commit messages

Short imperative subject, then a body explaining *why* rather than *what*
(the diff already shows what). Prefixes like `fix:`, `feat:`, `docs:`,
`perf:`, `ci:` are used but not enforced.

## Translations

Adding or fixing a language is one of the easiest ways to help:

1. Copy `android/app/src/main/res/values-en/strings.xml` to
   `values-XX/strings.xml` and translate the values (never the `name=` keys).
2. Add the language to the picker list in
   `android/app/src/main/java/com/ailauncher/app/ui/screens/AppearanceSettings.kt`.
3. Optionally translate the README into `docs/readme/README.XX.md`.

Please keep the four Hebrew news-brand names (Walla, Haaretz, Kan, Calcalist)
untranslated — they're the outlets' actual names.

## Reporting bugs

Include your Android version, device model, the app version (Settings →
About), and what you expected versus what happened. Screenshots help a lot.

For anything security-related, read [SECURITY.md](SECURITY.md) and report it
privately instead of opening a public issue.

## Code of conduct

Be decent to each other. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
