# Google Play Store — Submission Checklist

## ✅ Required assets (graphics live one level up, docs in this folder)

### Graphics
- [x] **Hi-res icon** — `../graphics/playstore-icon-512.png` (512×512, 32-bit PNG with alpha)
- [x] **Feature graphic** — `../graphics/feature-graphic-1024x500.png` (1024×500 JPG/PNG)
- [x] **Phone screenshots** — `../screenshots/google-play/01-home.png` to `09-settings.png` (1080×2160 each, captured from v10: Jewish calendar and Shabbat mode, search answers, app shortcuts, work profile and Private Space, wallpapers). APKPure-sized copies are in `../screenshots/apkpure/`, raw captures in `../screenshots/raw/`, and every other store's sizes in [stores/](stores/README.md).
- [ ] **Tablet screenshots** (optional, recommended) — render at 1600×2560 if you want to target tablets
- [ ] **Promo video** (optional) — 30-second YouTube link

### Documents
- [x] **Privacy Policy** — published at https://drummingbird1.github.io/NoveLauncher/privacy.html (source `../../docs/privacy.html`; copies in `PRIVACY_POLICY.md` / `.html`)
- [x] **Terms of Service** — published at https://drummingbird1.github.io/NoveLauncher/terms.html (copy in `TERMS_OF_SERVICE.md`)
- [x] **Store Listing copy** — `STORE_LISTING.md` (Hebrew + English, with the v10.1.0 release notes)
- [x] **Permission declarations** — `PLAY_POLICY_NOTES.md` (ready-to-paste answers for every sensitive permission)
- [x] ~~Google Drive OAuth clients~~ — not needed: Drive backup is switched off in 10.x (`DRIVE_BACKUP_ENABLED = false`). If Drive is ever turned on, follow `GOOGLE_DRIVE_SETUP.md` and register the **Play app signing key's** SHA-1 too (Play Console → Test and release → App integrity).

### Build
- [ ] **Signed AAB** — `release.yml` builds an APK only, so the Play bundle is built on your machine with [`build-play-aab.ps1`](build-play-aab.ps1), from a checkout of the release tag (see "Build the AAB" below). The signing passwords are typed into your own terminal, never into chat or the repo
- [ ] **Keystore file** — `android/release.keystore` (gitignored). Back it up; you can never replace it once published
- [x] **App version** — 14 / "10.1.0" in [android/app/build.gradle.kts](../../android/app/build.gradle.kts). Every upload to Play needs a higher `versionCode` than the last

## 📍 Where the Play Console stands (read-only check, 2026-10-31)

- App "NoveLauncher", package `com.ailauncher.app`: **draft** since 2026-04-25. Nothing has been sent to Google.
- App setup was 9 of 11 done. **Update 2026-10-31:** the privacy policy URL, the Advertising ID declaration (No) and Data safety are now saved in the Console (saved, not sent: they wait in the publishing overview, "סיכום השינויים לפרסום"), and the target audience was corrected (below), so App content lists no declaration waiting for action. Still to do: the signed AAB upload and, once the Console has seen the bundle, whatever it then asks for (expected: the Accessibility API and `QUERY_ALL_PACKAGES` declarations, which `PLAY_POLICY_NOTES.md` covers).
- **Managed publishing is off** for this app (publishing overview). After Google approves a release, it goes live on its own; turn managed publishing on first if you want to press Publish yourself.
- **Target audience was wrong.** Since 2026-04-25 the Console had *every* age group ticked (5 and under through 18+), which pulls in the Families Policy that a launcher with `QUERY_ALL_PACKAGES`, an accessibility service, usage access and notification access can't meet, and contradicts the privacy policy ("does not target children under 13"). Changed on 2026-10-31 to **13-15, 16-17 and 18+**, saved.
- Internal testing: testers chosen and a draft release "V1.0.0" exists, but **no bundle has ever been uploaded**. Open testing is a draft (178 countries) and, like closed testing and production, stays locked until the app setup is complete.
- Managed publishing may be on for this app (it was for PhoneLab, from the same account): if so, after Google approves someone must click **Publish** in the publishing overview ("סיכום השינויים לפרסום"). Check the setting before submitting.
- Any Billing, ads or tip-jar library would add a payments profile and make Play show the developer's address publicly. NoveLauncher has none; keep it that way.

**Order of work**

1. App content → **Privacy policy**: paste `https://drummingbird1.github.io/NoveLauncher/privacy.html`.
2. App content → **Data safety**: answers in the section below.
3. The other App content forms: Ads **No**; App access: everything works without an account; Target audience **13-15, 16-17, 18+** (fixed, see above); Content rating (answers below); **Advertising ID: No** (the bundle declares no `AD_ID` permission, see "Verified on the build"); Government, financial and health apps: **No**; the **Accessibility API** declaration with its video and the **`QUERY_ALL_PACKAGES`** declaration, both in `PLAY_POLICY_NOTES.md`.
4. Build the signed AAB (done: see the build section) and upload it to **Internal testing**. The app signing key is already fixed (next section).
5. Install from the internal-testing link and record the accessibility video from that Play-installed build.
6. Only then send the changes for review. A first release with an AccessibilityService and `QUERY_ALL_PACKAGES` can take longer than the usual 1–7 days.

## 🔑 Play App Signing — where it stands (2026-10-31)

When the app was created in April the Console generated a **Google app signing key** (SHA-256 `0C:35:A3:F9:…:5B:59:A7`) and registered an **upload key** that is *not* `android/release.keystore` (SHA-1 `15:BC:DA:C6:…:A9:0F:8C`; the release keystore dates from 2026-08-16 and is SHA-1 `42:71:55:2E:…:65:84:4F`, SHA-256 `9F:D1:48:BD:…:94:8D:F8:A1`).

1. **App signing key: replaced with the release key. Done.** Test and release → App signing → "החלפת המפתח" → "export and upload a key from a Java keystore": download Google's `pepk.jar` and the public encryption key from that dialog, run `java -jar pepk.jar --keystore=… --alias=Idan --output=… --include-cert --rsa-aes-encryption --encryption-key-path=…` in your own terminal (it prompts for both passwords), upload the zip, Save. The warning about installs and earlier uploads didn't matter here: installation base 0%, nothing uploaded. Result: the page shows `9F:D1:48:BD…`, so Play, GitHub Releases, Obtainium and the other stores share one signature and can update each other. Gotcha: pick the zip by its full path, the file chooser opens in Downloads next to `pepk.jar`.
2. **Upload key: still the April key, so the AAB is refused** ("signed with the wrong key", expected SHA-1 `15:BC:DA…`). The signing-key change didn't touch it; the optional "register a new upload key" step in that dialog was skipped and apparently matters. An **upload key reset request** was sent from the same page ("בקשה לאיפוס" → reason "I lost my upload key" → the public certificate of the release key as PEM, which can be extracted from the signed AAB with `keytool -printcert -rfc -jarfile app-release.aab`, no password needed). Google answers by email; until it approves, no AAB upload is accepted. The request showed no confirmation on the page, so watch the account's email, and retry the upload to find out.
3. Once the upload works, finish the release in Internal testing: the draft already carries the name `10.1.0` and the EN/HE release notes.

Back up `android/release.keystore` (password manager attachment plus one offline copy). Nobody — not Claude — should ever see its passwords.

## 📋 Steps to publish

1. ~~**Create developer account**~~ — done
2. ~~**Create new app**~~ — done (draft, see above); check the Console already has:
   - Name: NoveLauncher
   - Default language: English (US) — add Hebrew as a translation
   - Category: Personalization
3. **Upload AAB** — Test and release → Testing → **Internal testing** → the draft release → upload `app-release.aab`. Production comes after the review
4. **Fill out store listing** (use `STORE_LISTING.md`)
5. **Add graphics** — upload from `../graphics/` and `../screenshots/`
6. **Privacy policy URL** — https://drummingbird1.github.io/NoveLauncher/privacy.html
7. **Content rating** — fill out questionnaire (Everyone, no ads, no UGC)
8. **Target audience** — 13+
9. **Sensitive permissions** — App content → declare each one below, using the text in `PLAY_POLICY_NOTES.md`
10. **Data safety form** — see the answers below
11. **Submit for review** — typically 1–7 days; apps with an AccessibilityService declaration or `QUERY_ALL_PACKAGES` can take longer

## 🔐 Permissions that need a Play Console declaration

| Permission / component | Declaration | Notes |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Permitted use: **launcher** | A home-screen app must list every installed app |
| `PACKAGE_USAGE_STATS` | Usage access, core functionality | Ranking and the Statistics page; granted by the user in system settings |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notification access | Badge counts only, never notification content; optional |
| **AccessibilityService** (`LockScreenAccessibilityService`) — *new in v10* | **AccessibilityService API** declaration + video of the disclosure and the feature | Not an accessibility tool (`isAccessibilityTool="false"`), so Play requires the prominent in-app disclosure (the `gesture_lock_setup_body` string) shown before the user is sent to system settings. Optional, off by default |
| `ACCESS_HIDDEN_PROFILES` — *new in v10* | Private Space; only granted to the default launcher | See `PLAY_POLICY_NOTES.md`. Play may not ask for a form, but the use must be explainable if the reviewer asks |
| `EXPAND_STATUS_BAR` — *new in v10* | None (normal permission, granted at install) | Swipe down on the home screen opens notifications |
| `ACCESS_COARSE_LOCATION` | Covered by the Data safety form | Weather and Jewish-calendar times; optional |
| `READ_CONTACTS` | Covered by the Data safety form | Contacts in search, on the device only; optional |

## ✅ Verified on the build (unsigned `bundleRelease` of main at 14ac6c0, 2026-10-31)

- It builds: `app-release.aab`, 8.9 MB, `versionCode` 14 / "10.1.0", `minSdk` 26, `targetSdk` 36.
- The merged manifest has **no `AD_ID`, advertising or Billing permission**, so the Advertising ID declaration is **No** and nothing adds a payments profile.
- **No `foregroundServiceType` is declared** anywhere. The only extras beyond the table above are `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED` and `WAKE_LOCK`, all added by WorkManager (`androidx.work` 2.12.0) for the scheduled backup. If the Console's foreground-service question comes up, the answer is that the app declares no foreground service types.
- **Not verified yet:** the signature of the signed bundle (the script checks it) and the Console accepting the upload.

## 🎨 Content rating answers

For the IARC questionnaire:
- Violence: **No**
- Sex: **No**
- Profanity: **No**
- Gambling: **No**
- Drugs/alcohol/tobacco: **No**
- User-generated content: **No** (RSS feeds are pre-curated public sources)
- Shares user location: **No** — approximate location goes only to the weather service to fetch a forecast; it is never shown to or shared with other users
- In-app purchases: **No**
- Ads: **No**
- Result: **Everyone**

## 🛡️ Data safety form answers

Google defines "collected" as *transmitted off the device* — to anyone, including a third-party API. The weather request sends approximate coordinates to Open-Meteo, so location has to be declared even though it never reaches us.

**Does your app collect or share any of the required user data types?** **Yes** — one type:

- **Location → Approximate location**
  - Collected: **Yes** (sent to Open-Meteo in the weather request, only if the user granted location)
  - Shared: **Yes**, with a third party (the weather provider, Open-Meteo). Decided by the owner on 2026-10-31: the cautious answer, one extra line on the store page
  - Processed ephemerally: **No**. The request puts the coordinates in the URL, and we can't confirm Open-Meteo doesn't keep request logs, so claiming "ephemeral" would be a guess. (An earlier version of this file said Yes.)
  - Required or optional: **Optional** (denied → weather uses a default city)
  - Purpose: **App functionality**

Not collected (processed only on the device): app activity and installed apps (ranking, statistics, badges), contacts (search), photos picked for wallpapers. Backups to the user's own NAS are uploads the user sets up to their own storage (Drive backup is switched off in 10.x) — the developer never receives them — so they are not declared as collection. The RSS and update-check requests carry no user data.

**Is all of the user data collected by your app encrypted in transit?** **Yes** (the Open-Meteo request is HTTPS; the code has no cleartext URLs)

**Account creation:** "My app doesn't let users create an account". **Can users sign in with accounts created outside the app?** **No**. **Can users request that their data be deleted?** (optional) left unanswered: the developer keeps nothing off the device.

**Do you provide a way for users to request that their data is deleted?** Nothing is retained off the device; on the device, Clear storage or uninstalling removes everything.

## 📦 Build the AAB

Build from a checkout of the release tag, so the bundle matches the APK that was released. The script is not part of the tag, so run it from the checkout that has it and point `-RepoRoot` at the tag checkout (which also needs its own `android\local.properties`, the gitignored SDK path). Run it in **your own terminal**: it asks for the keystore and key passwords and sets them for that process only.

```powershell
git worktree add --detach ..\play-10.1.0 v10.1.0
.\assets\distribution\build-play-aab.ps1 -RepoRoot ..\play-10.1.0 -Keystore E:\AI\projects\nove-launcher\android\release.keystore
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`, after the script has checked its signature and printed the certificate fingerprint (it must match the released APK's) and the file hash. Upload it to Play Console → Internal testing; don't attach it to a public GitHub release. (Setting the `RELEASE_KEYSTORE_*` environment variables and running `.\gradlew.bat bundleRelease` from `android/` does the same, and Android Studio → **Build → Generate Signed Bundle / APK** works too; in every case use the existing release keystore, never a new one.)

## 📂 Where things live

Repo root has three folders: `docs/` (the published site), `android/` (the Gradle/Android Studio
project — open this in Android Studio, not the repo root) and `assets/`
(design source + everything needed to submit to Play Store). This
`distribution/` folder is a subfolder of `assets/`, sitting next to the
graphics it references.

```
assets/
├── generate_graphics.py      — Script that produced graphics/
├── graphics/
│   ├── playstore-icon-512.png      — App icon (Play Store hi-res)
│   ├── playstore-icon-1024.png     — Even higher res backup
│   ├── logo-monochrome-512.png     — Single-color logo
│   ├── logo-horizontal-1024x320.png — Icon + wordmark lockup
│   └── feature-graphic-1024x500.png — Banner shown at top of listing
├── screenshots/
│   ├── google-play/           — 1080×2160 store screenshots
│   ├── apkpure/               — APKPure-sized copies
│   └── raw/                   — Raw captures
└── distribution/
    ├── STORE_LISTING.md          — App description (HE + EN) + release notes
    ├── LISTING.md                — Hebrew-first listing + privacy/terms summaries
    ├── PRIVACY_POLICY.md / .html — Copies of the published privacy policy
    ├── TERMS_OF_SERVICE.md       — Copy of the published terms
    ├── PRIVACY_URL_GUIDE.md      — How the privacy policy is hosted
    ├── PLAY_POLICY_NOTES.md      — Play Console permission declarations
    ├── GOOGLE_DRIVE_SETUP.md     — OAuth clients for Drive backup (Drive is off in 10.x)
    ├── build-play-aab.ps1        — Builds and checks the signed AAB for Play
    ├── ALTERNATIVE_STORES.md     — Stores other than Google Play
    └── CHECKLIST.md              — This file

android/                        — Open THIS folder in Android Studio
├── app/                        — The launcher module
└── baselineprofile/            — Baseline Profile generator + startup benchmark
```

## 🚀 After submission

- Reviews typically take 1–7 days for first submission
- Subsequent updates usually clear within 24 hours
- If rejected, you'll receive specific feedback — address each item and resubmit
- Most common rejections for a launcher: missing privacy policy URL, a sensitive permission without a matching declaration, or an AccessibilityService disclosure that doesn't match what the service does
