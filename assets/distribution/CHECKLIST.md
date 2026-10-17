# Google Play Store — Submission Checklist

## ✅ Required assets (graphics live one level up, docs in this folder)

### Graphics
- [x] **Hi-res icon** — `../graphics/playstore-icon-512.png` (512×512, 32-bit PNG with alpha)
- [x] **Feature graphic** — `../graphics/feature-graphic-1024x500.png` (1024×500 JPG/PNG)
- [x] **Phone screenshots** — `../screenshots/google-play/01-welcome.png` to `10-apps.png` (1080×2160 each; APKPure-sized copies in `../screenshots/apkpure/`, raw captures in `../screenshots/raw/`). They show v9.3 — consider adding wallpapers, Private Space, gestures or the Jewish calendar for v10.
- [ ] **Tablet screenshots** (optional, recommended) — render at 1600×2560 if you want to target tablets
- [ ] **Promo video** (optional) — 30-second YouTube link

### Documents
- [x] **Privacy Policy** — published at https://drummingbird1.github.io/NoveLauncher/privacy.html (source `../../docs/privacy.html`; copies in `PRIVACY_POLICY.md` / `.html`)
- [x] **Terms of Service** — published at https://drummingbird1.github.io/NoveLauncher/terms.html (copy in `TERMS_OF_SERVICE.md`)
- [x] **Store Listing copy** — `STORE_LISTING.md` (Hebrew + English, with v10.0.0 release notes)
- [x] **Permission declarations** — `PLAY_POLICY_NOTES.md` (ready-to-paste answers for every sensitive permission)
- [ ] **Google Drive OAuth clients** — `GOOGLE_DRIVE_SETUP.md`. Register the **Play app signing key's** SHA-1 too (Play Console → Test and release → App integrity), or Drive backup fails with `DEVELOPER_ERROR` on Play installs.

### Build
- [ ] **Signed APK or AAB** — `.\gradlew.bat bundleRelease` with the `RELEASE_KEYSTORE_*` environment variables set (see the release-signing notes in `../../CLAUDE.md`), or push a `vX.Y.Z` tag and let `release.yml` build it
- [ ] **Keystore file** — back this up; you can never replace it once published
- [ ] **App version** — check `versionCode`/`versionName` in [android/app/build.gradle.kts](../../android/app/build.gradle.kts) (currently 12 / "9.3.0"; v10 ships as 13 / "10.0.0")

## 📋 Steps to publish

1. **Create developer account** — https://play.google.com/console/signup ($25 one-time)
2. **Create new app**
   - Name: NoveLauncher
   - Default language: English (US) — add Hebrew as a translation
   - Category: Personalization
3. **Upload AAB** — Production → Create new release → Upload bundle
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
  - Shared: **No** (it goes to the weather service only to answer that request)
  - Processed ephemerally: **Yes** (used for the request, not stored off the device)
  - Required or optional: **Optional** (denied → weather uses a default city)
  - Purpose: **App functionality**

Not collected (processed only on the device): app activity and installed apps (ranking, statistics, badges), contacts (search), photos picked for wallpapers. Backups to the user's own Google Drive or NAS are uploads the user sets up to their own storage — the developer never receives them — so they are not declared as collection. The RSS and update-check requests carry no user data.

**Is all of the user data collected by your app encrypted in transit?** **Yes** (the Open-Meteo request is HTTPS)

**Do you provide a way for users to request that their data is deleted?** Nothing is retained off the device; on the device, Clear storage or uninstalling removes everything.

## 📦 Build the AAB

From `android/` with the release-signing environment variables set:

```powershell
.\gradlew.bat bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab` — upload this to Play Console. (Android Studio → **Build → Generate Signed Bundle / APK** works too; pick the existing release keystore, never a new one.)

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
    ├── GOOGLE_DRIVE_SETUP.md     — OAuth clients for Drive backup
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
