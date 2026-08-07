# Google Play Store — Submission Checklist

## ✅ Required assets (graphics live in ../assets/, docs in this folder)

### Graphics
- [x] **Hi-res icon** — `../assets/graphics/playstore-icon-512.png` (512×512, 32-bit PNG with alpha)
- [x] **Feature graphic** — `../assets/graphics/feature-graphic-1024x500.png` (1024×500 JPG/PNG)
- [x] **Phone screenshots** — `../assets/screenshots/01-home.png` to `05-categories.png` (1080×1920 each — Play accepts 320–3840px wide)
- [ ] **Tablet screenshots** (optional, recommended) — render at 1600×2560 if you want to target tablets
- [ ] **Promo video** (optional) — 30-second YouTube link

### Documents
- [x] **Privacy Policy** — `PRIVACY_POLICY.md` — host this on a public URL (e.g., your website, GitHub Pages)
- [x] **Terms of Service** — `TERMS_OF_SERVICE.md` — same as above
- [x] **Store Listing copy** — `STORE_LISTING.md` (Hebrew + English)

### Build
- [ ] **Signed APK or AAB** — generate from Android Studio → Build → Generate Signed Bundle/APK
- [ ] **Keystore file** — back this up; you can never replace it once published
- [ ] **App version** — check `versionCode`/`versionName` in [app/build.gradle.kts](../app/build.gradle.kts) (currently 10 / "9.1.0")

## 📋 Steps to publish

1. **Create developer account** — https://play.google.com/console/signup ($25 one-time)
2. **Create new app**
   - Name: NoveLauncher
   - Default language: English (US) — add Hebrew as a translation
   - Category: Personalization
3. **Upload AAB** — Production → Create new release → Upload bundle
4. **Fill out store listing** (use `STORE_LISTING.md`)
5. **Add graphics** — upload from `../assets/graphics/` and `../assets/screenshots/`
6. **Privacy policy URL** — paste link to your hosted privacy policy
7. **Content rating** — fill out questionnaire (Everyone, no ads, no UGC)
8. **Target audience** — 13+
9. **Data safety form** — declare:
   - Personal info: NOT collected
   - Location: collected on-device only (for weather)
   - App activity: NOT collected (used on-device for AI)
   - All data encrypted in transit (HTTPS only)
10. **Submit for review** — typically 1–7 days

## 🎨 Content rating answers

For the IARC questionnaire:
- Violence: **No**
- Sex: **No**
- Profanity: **No**
- Gambling: **No**
- Drugs/alcohol/tobacco: **No**
- User-generated content: **No** (RSS feeds are pre-curated public sources)
- Shares user location: **Yes — only for weather widget, not transmitted to us**
- In-app purchases: **No**
- Ads: **No**
- Result: **Everyone**

## 🛡️ Data safety form answers

**Does your app collect or share any of the required user data types?** No

**Is all of the user data collected by your app encrypted in transit?** Yes

**Do you provide a way for users to request that their data is deleted?** Yes (uninstalling the app removes all data, since nothing is collected on servers)

## 📦 Build the AAB

In Android Studio:
1. **Build** menu → **Generate Signed Bundle / APK**
2. Choose **Android App Bundle**
3. Create or select a keystore
   - Save the keystore file securely — you cannot reset it
   - Use a strong password (24+ chars recommended)
4. **Release** build variant
5. Output: `app/release/app-release.aab` — upload this to Play Console

## 📂 Where things live

Docs (this folder) and graphics (`../assets/`) are split so the binary assets
stay out of the way when browsing the submission text, and out of the Android
Studio project tree entirely (neither folder is under `app/`).

```
distribution/
├── STORE_LISTING.md          — App description (HE + EN)
├── PRIVACY_POLICY.md         — Privacy policy
├── TERMS_OF_SERVICE.md       — Terms of service
├── PRIVACY_URL_GUIDE.md      — How to host the privacy policy publicly
├── LISTING.md                — Extended listing copy
└── CHECKLIST.md              — This file

assets/
├── generate_graphics.py      — Script that produced graphics/
├── graphics/
│   ├── playstore-icon-512.png      — App icon (Play Store hi-res)
│   ├── playstore-icon-1024.png     — Even higher res backup
│   ├── logo-monochrome-512.png     — Single-color logo
│   ├── logo-horizontal-1024x320.png — Icon + wordmark lockup
│   └── feature-graphic-1024x500.png — Banner shown at top of listing
└── screenshots/
    ├── 01-home.png
    ├── 02-settings.png
    ├── 03-themes.png
    ├── 04-apps.png
    └── 05-categories.png
```

## 🚀 After submission

- Reviews typically take 1–7 days for first submission
- Subsequent updates usually clear within 24 hours
- If rejected, you'll receive specific feedback — address each item and resubmit
- Most common rejection: missing privacy policy URL or sensitive permission justification
