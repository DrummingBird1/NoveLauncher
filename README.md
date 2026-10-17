<div align="center">

<img src="assets/graphics/readme-banner.png" alt="NoveLauncher — a private, on-device AI launcher for Android" width="100%">

<br>

**A private, on-device Android launcher that learns your habits — without sending them anywhere.**

[![Version](https://img.shields.io/badge/version-9.3.0-7C7CFF?style=for-the-badge)](https://github.com/DrummingBird1/NoveLauncher/releases/latest)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-4ECDC4?style=for-the-badge&logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![CI](https://img.shields.io/github/actions/workflow/status/DrummingBird1/NoveLauncher/android.yml?branch=main&style=for-the-badge&label=CI)](https://github.com/DrummingBird1/NoveLauncher/actions/workflows/android.yml)

[**Website**](https://drummingbird1.github.io/NoveLauncher/) ·
[**Download**](https://github.com/DrummingBird1/NoveLauncher/releases/latest) ·
[**Privacy Policy**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[**Changelog**](CHANGELOG.md) ·
[**Contributing**](CONTRIBUTING.md)

**Read this in other languages:**
[עברית](docs/readme/README.he.md) ·
[العربية](docs/readme/README.ar.md) ·
[Français](docs/readme/README.fr.md) ·
[Русский](docs/readme/README.ru.md) ·
[Español](docs/readme/README.es.md) ·
[Deutsch](docs/readme/README.de.md)

</div>

---

## What it is

NoveLauncher replaces your Android home screen. It ranks and groups your apps
based on how you actually use them — what you open, when you open it, and how
often — so the things you need are already in front of you.

The ranking runs **entirely on your device**. No account, no server, no
analytics, no ads. Your usage data never leaves your phone.

## Features

| | |
|---|---|
| 🧠 **Suggestions that learn** | Apps ranked by recency, frequency, time of day and category; "Recommended now" also learns from which suggestions you actually tap — all computed locally |
| 📁 **Smart folders** | Automatic grouping into 11 categories, no manual sorting |
| 💼 **Work profile & Private Space** | Work apps with their badge and a pause switch; Android 15's Private Space in its own lockable section, never mixed into suggestions and hidden from search while locked |
| ⚡ **App shortcuts** | Long-press an app for its own shortcuts — a new message, a saved route — straight from the home screen |
| 🔍 **Smart search** | Apps, contacts and settings from one box. Hebrew ↔ English matching ("וואטסאפ" finds WhatsApp), typo tolerance and initials ("gm" → Google Maps), plus a calculator and unit conversion |
| 🕯️ **Jewish calendar & Shabbat mode** | Hebrew date, holidays (Israel or diaspora schedule), candle-lighting and Havdalah times calculated on the phone; Shabbat mode hides distracting suggestions and badges from candle lighting to nightfall |
| 🖼️ **Wallpapers** | Your own photos or built-in gradients — one for everything, per page, by hour, by weekday, or your system (live) wallpaper — with brightness, contrast, saturation, warmth, blur and vignette |
| 👆 **Gestures** | Swipe up for apps, swipe down for notifications, double-tap to turn the screen off |
| 🎨 **Deep theming** | 12 colour presets, Material You dynamic colour, custom colours, 16 fonts, 12 icon shapes |
| 🧷 **Icon packs** | Apply any Nova / ADW / Lawnchair-compatible icon pack; apps it doesn't cover keep their own icon |
| 🔒 **App lock** | PIN, password, pattern or biometric lock on individual apps, plus a private folder and hidden apps |
| 🌍 **7 languages** | Hebrew, English, Arabic, French, Russian, Spanish, German — fully RTL-aware |
| 📰 **News feed** | Built-in sources plus your own custom RSS feeds |
| 💾 **Backups** | Local or your own NAS/WebDAV, on a schedule, optionally password-encrypted |
| 📊 **Usage statistics** | Screen time and app usage by day, week or month, read from Android on your device |
| 🧩 **Widgets & dock** | Standard Android widgets (with their setup screens, resizable) and a pinned bottom dock |

## Screenshots

<div align="center">

<img src="assets/screenshots/google-play/01-home.png" width="24%" alt="Home screen">
<img src="assets/screenshots/google-play/04-apps-profiles.png" width="24%" alt="App drawer">
<img src="assets/screenshots/google-play/07-themes.png" width="24%" alt="Themes">
<img src="assets/screenshots/google-play/08-security.png" width="24%" alt="Security">

</div>

## Privacy

This is the whole point of the project, so it's worth being precise:

- **Nothing is collected.** No analytics, no telemetry, no crash reporting is
  enabled in shipped builds, no account, no ads.
- **Usage data stays local.** App usage is read from Android's own usage
  statistics and processed on your device; what the suggestions learn is kept
  in a local database. None of it is uploaded.
- **Four features use the network — nothing else does:**
  - **Weather** — asks [Open-Meteo](https://open-meteo.com) for the current
    conditions. It sends your approximate coordinates only if you grant
    location permission; without it, it asks for a default city.
  - **News** — the News page fetches the RSS feeds you have enabled, directly
    from each publisher.
  - **Backups** — only if you choose a NAS: your own server, over HTTPS.
  - **Update check** — off by default. When you turn it on, the app asks
    GitHub's releases API for the latest version at most once a day. Nothing
    about you or your device is sent, and nothing is downloaded automatically.

  Everything else stays on your device.
- **Backups are yours.** On the phone or on your own NAS. Exports can be
  encrypted with a password only you hold.
- **The Jewish calendar is computed on the phone.** Holiday, candle-lighting
  and Havdalah times use your approximate location if you allow it (a default
  city otherwise), and are never sent anywhere.
- **The gestures helper reads nothing.** Double-tap to turn the screen off uses
  an optional accessibility service that performs exactly two actions (screen
  off, open notifications) and cannot see what's on your screen.

Full detail: [**Privacy Policy**](https://drummingbird1.github.io/NoveLauncher/privacy.html) ·
[Terms of Service](https://drummingbird1.github.io/NoveLauncher/terms.html)

> **On app lock:** the app lock, private folder and hidden apps are a
> *deterrent*, not a sandbox. A locked app is still reachable from recents,
> notifications, another launcher or ADB — that's true of every third-party
> launcher. For real isolation, use Android's built-in Work Profile or Private
> Space (Android 15+), both of which NoveLauncher supports.

## Download

Grab the signed APK from the [**latest release**](https://github.com/DrummingBird1/NoveLauncher/releases/latest).

You'll need to allow installation from unknown sources for whichever app you
download it with. Every release is signed with the same key, so updates
install cleanly over each other.

<a href="https://github.com/ImranR98/Obtainium"><img src="docs/assets/badge-obtainium.png" alt="Get it on Obtainium" height="62"></a>

**Automatic updates:** [Obtainium](https://github.com/ImranR98/Obtainium) installs NoveLauncher straight from
these GitHub Releases and keeps it up to date. In Obtainium, tap **Add App** and
paste `https://github.com/DrummingBird1/NoveLauncher`.

### Requirements

- Android 8.0 (API 26) or newer
- ~25 MB of storage

A few features need more: Private Space needs Android 15, and double-tap to
turn the screen off needs Android 9. Private Space, app shortcuts and pausing
work apps work only while NoveLauncher is your default home app.

## Build from source

```bash
git clone https://github.com/DrummingBird1/NoveLauncher.git
cd NoveLauncher/android
./gradlew assembleDebug
```

Open the **`android/`** folder in an Android Studio version that supports
Android Gradle Plugin 9.4 — not the repository root, which is not a Gradle
project. Gradle needs **JDK 21** (set it as the Gradle JDK in Android Studio);
the app itself compiles to Java 17 bytecode.

```bash
./gradlew testDebugUnitTest      # unit, Robolectric and screenshot tests
./gradlew verifyPaparazziDebug   # screenshot tests only
./gradlew assembleRelease        # R8-minified release build
```

Release signing is opt-in through environment variables
(`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD`). Without them the release build is simply unsigned —
no keystore is stored in this repository.

## Architecture

Single-activity Jetpack Compose UI, Hilt dependency injection, layered
`ui / domain / data` structure with Room and DataStore for persistence.

```
android/app/src/main/java/com/ailauncher/app/
├── ui/          Compose screens, ViewModel, activities
├── domain/      models, use cases, repository interfaces
├── data/        repositories, Room database, backups, weather, ML ranking
├── security/    app lock, biometrics, crypto
└── di/          Hilt modules
```

Deeper notes on conventions, pitfalls and where to start a given task live in
[CLAUDE.md](CLAUDE.md) and [AGENTS.md](AGENTS.md); architecture decisions are
recorded in [assets/docs/adr/](assets/docs/adr/).

## Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md)
for how to set up the project, what the CI checks, and the conventions the
codebase follows. Security issues: please read [SECURITY.md](SECURITY.md)
first and report privately rather than opening a public issue.

## Support the project

NoveLauncher is free, ad-free and has no in-app purchases. If you'd like to
support development:

<div align="center">

[![Patreon](https://img.shields.io/badge/Patreon-Support-FF424D?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/c/Solvaris)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Support-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/solvaris)

</div>

## Contact

Questions, bugs or feature requests: **solvaris2@gmail.com** or
[open an issue](https://github.com/DrummingBird1/NoveLauncher/issues).

## License

See [LICENSE](LICENSE). The source is published for transparency and review —
it is not released under an open-source licence.
