# NoveLauncher — Google Play Store Listing (short form)

The short-form listing with the privacy and terms summaries for store forms.
The full copy, release notes and store URLs live in
[STORE_LISTING.md](STORE_LISTING.md); keep the two in agreement. Hebrew
versions: [LISTING.he.md](LISTING.he.md) and
[STORE_LISTING.he.md](STORE_LISTING.he.md).

## App Name
NoveLauncher - Smart Launcher

## Short Description (80 chars)
Smart Android launcher — AI that organizes your phone around your habits

## Full Description (4000 chars max)

🚀 NoveLauncher — The Smart Launcher That Learns You

NoveLauncher is a private Android launcher that organizes your home screen around how you actually use your phone. It learns your usage habits on the device and puts the right apps in front of you at the right time — no account and no setup.

✨ What makes NoveLauncher special?

🧠 On-device AI — Ranks apps by time of day, frequency, recency and category, and "Recommended now" learns from which suggestions you actually open. Everything runs on the device — no usage data is sent to any server.

📂 Smart Folders — Apps are auto-sorted into 11 categories: Work, Social, Entertainment, Games, Shopping and more. No manual sorting needed.

💼 Work Profile & Private Space — Work apps keep their badge and a pause switch; Android 15's Private Space gets its own lockable section.

⚡ App Shortcuts — Long-press an app to open its shortcuts.

🔍 Smart Search — Hebrew and English, forgiving of typos and initials, with a calculator and unit conversion.

🕯️ Jewish Calendar & Shabbat Mode — Hebrew date, holidays, candle-lighting and Havdalah times calculated on the phone; Shabbat mode hides distracting suggestions and notification badges.

🖼️ Wallpapers — Your own photos or gradients: fixed, per page, by hour, by weekday, or a live wallpaper. Brightness, contrast, saturation, warmth, blur and vignette.

👆 Gestures — Swipe up for apps, swipe down for notifications, double-tap to turn the screen off.

🎨 Full Customization — 12 theme presets, Material You, custom colors, 16 fonts, 12 icon shapes, icon packs, clock styles and a live preview.

📰 News Page — Built-in RSS with 12 sources (ynet, Google News, BBC, CNN and more) plus your own feeds.

🔒 Advanced Security — App lock with PIN, password, pattern, fingerprint or face authentication. Hidden private folder. Icon layout lock.

💾 Backups — Local or to your own NAS, with scheduling and optional password encryption.

🧩 Widgets — Add any system widget, set it up, and make it one to four rows tall.

📱 Custom Pages — Create new pages, reorder them, turn pages on or off.

👁️ Privacy — No account, no analytics, no ads. The source code is open to read on GitHub.

⚡ Battery Friendly — Refreshes every 15 minutes and only while the home screen is visible; efficient Jetpack Compose UI.

## Category
Personalization

## Tags
launcher, home screen, AI, smart, customization, themes, privacy, hebrew,
work profile, private space, jewish calendar

---

# Privacy Policy — summary

The full, binding policy is published at
<https://drummingbird1.github.io/NoveLauncher/privacy.html> (source:
[`docs/privacy.html`](../../docs/privacy.html)). This is a summary for store
forms that ask for one.

### Data Collection
NoveLauncher does NOT collect personal data and has no servers of its own.

- App usage statistics are read from Android's UsageStatsManager and processed on the device
- App categorization uses on-device heuristics (no cloud AI)
- Settings are stored locally using Android DataStore; security and backup settings are encrypted
- Backups go only to destinations YOU choose (local file or your own NAS) and are encrypted only if you set a backup password

### Network connections
- Weather: Open-Meteo, with approximate coordinates only if location permission is granted (a default city otherwise)
- News: the RSS feeds enabled on the News page, fetched directly from each publisher
- Backups (opt-in): your own NAS over HTTPS
- Update check (opt-in, off by default): GitHub's releases API, at most once a day
- Nothing else. No analytics, tracking or advertising SDKs.

### Permissions Explained
- PACKAGE_USAGE_STATS: to rank apps from your usage and show the Statistics page
- QUERY_ALL_PACKAGES: to show every installed app, as a launcher must
- ACCESS_HIDDEN_PROFILES: to show Android 15's Private Space in its own section
- BIND_NOTIFICATION_LISTENER_SERVICE (optional): unread badges — counts only, never content
- ACCESS_COARSE_LOCATION (optional): local weather and Jewish-calendar times
- READ_CONTACTS (optional): contacts in search results, on the device only
- USE_BIOMETRIC: fingerprint/face app lock, handled by Android
- INTERNET: weather, news feeds, the backups you set up and the optional update check
- EXPAND_STATUS_BAR: swipe down on the home screen to open notifications
- Accessibility service (optional, off by default): screen off on double-tap and open notifications — it cannot read screen content

### Contact
solvaris2@gmail.com

---

# Terms of Service — summary

The full terms are published at
<https://drummingbird1.github.io/NoveLauncher/terms.html> (source:
[`docs/terms.html`](../../docs/terms.html)).

1. ACCEPTANCE: By using NoveLauncher, you agree to these terms.

2. LICENSE: NoveLauncher grants you a personal, non-exclusive, non-transferable, revocable licence to use the app. The source code is published for transparency and review under the repository's LICENSE — it is not an open-source licence.

3. PRIVACY: We do not collect personal data. See the Privacy Policy for details.

4. AS-IS: The app is provided "as is" without warranties of any kind. We are not responsible for data loss, device issues, or any damages arising from use of the app. Jewish-calendar times are a convenience, not a religious authority.

5. MODIFICATIONS: We reserve the right to update the app and these terms. Continued use after changes constitutes acceptance.

6. INTELLECTUAL PROPERTY: NoveLauncher and its design are protected by copyright. You may not sell, sublicense or redistribute the app.

7. TERMINATION: We may terminate access to the app at any time for violation of these terms.

8. GOVERNING LAW: These terms are governed by the laws of Israel.

9. CONTACT: solvaris2@gmail.com
