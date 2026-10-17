# Privacy Policy — NoveLauncher

**Effective date:** April 25, 2026
**Last updated:** October 17, 2026

> The published version is <https://drummingbird1.github.io/NoveLauncher/privacy.html>
> (source: [`docs/privacy.html`](../../docs/privacy.html)). Keep this file and
> [`PRIVACY_POLICY.html`](PRIVACY_POLICY.html) in sync with it.

NoveLauncher ("we", "our", or "the app") is operated as a personal-use Android launcher application. This privacy policy explains what data the app accesses, how it is used, and your rights as a user.

## TL;DR

**NoveLauncher has no servers and no accounts, and collects nothing about you.** Your usage data is processed on your device. The app goes online only for weather, the news feeds you enable, backups you choose, and an update check you can turn on — every connection is listed in section 3.

## 1. Information the App Uses on Your Device

To provide its functionality, NoveLauncher reads the following on your device. None of it is sent to us — we have no server to send it to.

### 1.1 App Usage Statistics
- **What:** Which apps you open, when, and for how long
- **Permission:** `PACKAGE_USAGE_STATS` ("Usage access", which you grant in Android settings)
- **Why:** To rank your apps, predict what you'll need next, and show the Statistics page
- **Where it goes:** Read from Android's usage-statistics service and processed on the device. The app keeps its own learning data — how often each suggestion was shown and how often you tapped it — in a local database. You can reset it from the Statistics page.
- **Sharing:** Never transmitted anywhere

### 1.2 Installed Apps, Work Profile and Private Space
- **What:** Names, package IDs, icons and shortcuts of installed apps, including apps in a work profile, a cloned-apps profile, or Android 15's Private Space
- **Permissions:** `QUERY_ALL_PACKAGES`, `ACCESS_HIDDEN_PROFILES` (Private Space; only effective while NoveLauncher is your default home app)
- **Why:** To display and launch them. Private Space apps appear only in their own section and only while the space is unlocked; they are never used for suggestions.
- **Sharing:** Never transmitted

### 1.3 Notifications (Optional)
- **What:** Which app posted a notification and how many are active — NOT their content
- **Permission:** Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`), which you enable manually
- **Why:** To show unread badges on app icons
- **Sharing:** Never transmitted

### 1.4 Location (Optional)
- **What:** The approximate location Android already has cached. The app never asks for a new location fix.
- **Permission:** `ACCESS_COARSE_LOCATION`
- **Why:** Local weather, and the holiday, candle-lighting and Havdalah times of the Jewish calendar
- **Sharing:** The weather request sends the coordinates to Open-Meteo (see section 3). Jewish-calendar times are calculated on the device and never sent anywhere.
- **Opt out:** Don't grant the permission. Weather and calendar times then use a default city (Tel Aviv).

### 1.5 Contacts (Optional)
- **What:** Contact names and phone numbers
- **Permission:** `READ_CONTACTS`
- **Why:** To include contacts in search results
- **Sharing:** Never transmitted

### 1.6 Biometric Authentication (Optional)
- **What:** Fingerprint or face authentication results (only "success/failure", not biometric data itself)
- **Permission:** `USE_BIOMETRIC`
- **Why:** To unlock locked apps and the launcher
- **Sharing:** Handled entirely by Android system, never accessed by us

### 1.7 Wallpaper Photos (Optional)
- **What:** Only the images you pick in Android's photo picker, which gives the app access to that one image — not to the rest of your gallery
- **Where it goes:** A downscaled copy is kept in the app's private storage so the wallpaper survives the original being moved or deleted
- **Sharing:** Never transmitted, and not included in settings exports or backups

### 1.8 Gestures Accessibility Service (Optional)
- **What it does:** The "NoveLauncher gestures" service performs exactly two actions, and only when you make the gesture: turning the screen off (double-tap on the home screen) and opening the notification shade (swipe down)
- **What it cannot do:** It is declared without permission to retrieve window content, so it cannot read what is on your screen or what you type, and it collects no data
- **Permission:** `BIND_ACCESSIBILITY_SERVICE`. It stays off until you enable it yourself in Android's Accessibility settings, after an explanation in the app. Without it, swipe-down still works through Android's status-bar API (`EXPAND_STATUS_BAR`).
- **Sharing:** Nothing to share — it reads and stores nothing

### 1.9 Headphones
- **What:** Whether headphones are connected right now (no permission needed)
- **Why:** A small ranking hint — for example, media apps are more likely to be useful with headphones on
- **Sharing:** Checked at the moment of ranking; not stored, never transmitted

## 2. Optional Backups

Backups are off unless you set them up. A backup contains your launcher settings as a JSON file — including the hashed (never plain-text) lock credentials and the list of locked and hidden apps. If you set a backup password, the file is encrypted with AES-256-GCM using a key derived from that password. Wallpaper images are not included.

- **Local file:** Saved to your device's Downloads folder. Never transmitted.
- **Google Drive:** You connect your own Google account in Settings → Backup through Google's consent screen (Google Identity Services, provided by Google Play services). The app requests only two scopes: `drive.file` — it can create and see only the files it creates itself, never the rest of your Drive — and `email`, used only to show which account is connected. Your Google password never passes through the app. Google Play services issues short-lived access tokens when a backup runs; the app stores only the connected account's email address on your device. "Disconnect" revokes the grant with Google.
- **NAS / WebDAV:** You provide your own server address. The file is sent to that address only, over HTTPS (plain HTTP is refused). The NAS password is stored encrypted on your device and is never included in exported files.
- **OneDrive and Box** are not available in the app.

## 3. Network Connections

These are all the connections NoveLauncher itself makes:

| Service | When | Data sent |
|---|---|---|
| Open-Meteo (`api.open-meteo.com`) | When the launcher starts, for the weather line on the home screen | Your approximate latitude/longitude if you granted location permission; otherwise the coordinates of the default city |
| RSS news feeds (the publishers you enable) | When the News page loads. By default ynet, Walla and Google News Israel are enabled; you can change them and add your own feeds | No personal data — a standard request for the feed |
| Google Drive (opt-in, `www.googleapis.com`) | When a backup to Drive runs | Your backup file, with an access token for your account |
| Google Identity Services (opt-in, via Google Play services) | When you connect Drive, and before each Drive backup to get a fresh token | Handled by Google Play services under Google's privacy policy; the app receives an access token and your account's email address |
| Your NAS (opt-in, the address you enter) | When a backup to the NAS runs | Your backup file and the NAS username/password you entered, over HTTPS |
| GitHub releases API (opt-in, `api.github.com`) | At most once a day while "Check for updates automatically" is on (off by default), or when you tap "Check now" in Settings → About | No personal data — the request names the app version in its User-Agent. The app only shows a link to the release; it never downloads or installs anything itself |

Like any internet request, each of these reveals your IP address to the server it reaches; those services' own privacy policies govern what they do with it. Links you open — news articles, a web search, the website or store page from Settings → About — open in your browser or the relevant app, not in the launcher. Widgets on your home screen are drawn by the app that provides them, under that app's own policy.

Shipped builds contain no analytics, advertising or tracking SDKs. The source code includes an optional crash-reporting library, but no reporting endpoint is configured in any published build, so it is inactive and sends nothing.

## 4. Data Retention

- All data is stored locally on your device
- Reset what suggestions have learned from the Statistics page
- Clear all data via Android Settings → Apps → NoveLauncher → Storage → Clear Storage
- Uninstalling removes everything on the device. Backup files you saved to your Drive or NAS stay there until you delete them
- Revoke Google Drive access with Settings → Backup → Disconnect, or from your Google Account

## 5. Children's Privacy

NoveLauncher does not target children under 13. We do not knowingly collect any data from children.

## 6. Your Rights (GDPR / CCPA)

Since we do not collect or store any data on servers of our own, there is nothing for us to provide, delete, or correct on your behalf. Data on your device is under your control, and so are any backup files in your own Drive or NAS.

## 7. Changes to This Policy

If this policy changes, we will update the "Last updated" date at the top. Material changes will be highlighted in the app's release notes.

- **October 17, 2026:** Covers what NoveLauncher 10 adds — the optional update check, the Google Drive connection details, the gestures accessibility service, work profile and Private Space, wallpaper photos, the Jewish calendar's use of location, and the headphones ranking hint. Every network connection is now listed in section 3. OneDrive and Box, which the app does not offer, were removed.

## 8. Contact

Questions or concerns? Email us at:
**solvaris2@gmail.com**

---

By using NoveLauncher, you confirm that you have read and understood this privacy policy.
