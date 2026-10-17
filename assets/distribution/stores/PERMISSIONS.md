# Permission justifications (all stores)

What NoveLauncher asks for and why, worded for store reviewers. Every store
here either has a free-text "notes for the reviewer" field or asks about
sensitive permissions in its review emails, and these answers fit both. A
paste-ready version is the "Comments to the certification team" / "Remarks"
block in each kit's `listing.en.md`. Keep this file in step with the Google
Play answers in [../PLAY_POLICY_NOTES.md](../PLAY_POLICY_NOTES.md), which has
longer ready-to-paste text for each of these (it works verbatim for any store), and with
[AndroidManifest.xml](../../../android/app/src/main/AndroidManifest.xml).

**Every store needs one fact up front:** NoveLauncher is a home-screen app.
Most of these permissions only make sense for a launcher, and the app does
nothing useful until it's set as the default Home app.

## Sensitive permissions

| Permission | Required? | Why | Where the data goes |
| --- | --- | --- | --- |
| `PACKAGE_USAGE_STATS` (Usage access) | Explained during onboarding, then granted in Android's Usage access settings. Optional: the launcher works without it, just without usage-based ranking and statistics. | Ranks apps in "Recommended now" by time of day, frequency and recency, and feeds the Usage statistics page | Read and processed on the device only. Never sent. |
| `QUERY_ALL_PACKAGES` | Required | A launcher's core job is listing and searching every installed app. The app grid itself comes from `LauncherApps`; full visibility is needed by the Usage statistics page (apps without a launcher icon), the app pickers for lock/hidden apps, and icon-pack discovery. | Stays on the device |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (Notification access) | Optional, user-enabled | Counts unread notifications per app to draw badges on icons | Reads only the posting app's package name. Per-app counts (package, number, time of the latest) are kept in the launcher's local database; titles and text are never read, stored or sent. Ongoing notifications are skipped. |
| `ACCESS_COARSE_LOCATION` | Optional, runtime prompt | Local weather under the clock, and sunset-based candle-lighting / Havdalah times | Coordinates go to Open-Meteo for the weather request only. Shabbat times are calculated on the device. Without the permission, times default to Tel Aviv. |
| `READ_CONTACTS` | Optional, runtime prompt from search | Shows matching contacts in search results | Read on the device only |
| AccessibilityService (`LockScreenAccessibilityService`) | Optional, off until the user enables it | Performs exactly two global actions, and only when the user makes the gesture: screen off (double-tap on the home screen) and open the notification shade (swipe down) | Reads no window content (`canRetrieveWindowContent="false"`, `isAccessibilityTool="false"`). The disclosure (`gesture_lock_setup_body`, all 7 locales) appears as a dialog on the home screen when the user double-taps, and as a card in Settings → Gestures, before the user is sent to Accessibility settings. |
| `ACCESS_HIDDEN_PROFILES` | Install-time | Shows Android 15+ Private Space apps in their own lockable section (Android only grants it to the default home app) | Stays on the device |
| `EXPAND_STATUS_BAR` | Install-time (normal) | Swipe down on the home screen opens the notification shade when the accessibility helper is off | n/a |
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Install-time (normal) | Optional fingerprint/face unlock for apps the user locked | Handled by Android's BiometricPrompt. The app never sees biometric data. |
| `POST_NOTIFICATIONS` | Optional, runtime prompt (Android 13+) | Only to report a scheduled backup that failed | n/a |
| `READ_EXTERNAL_STORAGE` (Android 12 and older only) | Optional | Restoring a settings backup the user picked from storage | Read on the device only |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Install-time | Weather (Open-Meteo), RSS news feeds the user picks, backups the user sets up (Google Drive, own NAS), optional once-a-day update check (GitHub API) | Only to those services. There are no ads, analytics or tracking SDKs. |
| `BIND_APPWIDGET`, `VIBRATE` | Install-time | Hosting home-screen widgets; haptic feedback | n/a |

## Things reviewers sometimes ask

- **Crash reporting:** the Sentry library is compiled in, but no DSN is
  configured in any build, so it never starts and sends nothing.
- **App lock** is described in the listing as a deterrent, not a security
  guarantee. No third-party launcher can stop an app from being opened from
  notifications, the recent-apps screen or another launcher.
- **Update check** (Settings → About, off by default): at most once a day it
  asks GitHub's public API for the latest release number and, if newer, shows
  a link to the release page. The app never downloads or installs anything
  itself. When a store installed the app (Google Play, Galaxy Store,
  AppGallery, Aptoide, Uptodown, APKPure), the check is hidden entirely and
  never goes online, and Rate opens that store.
- **Google Drive backup** uses Google Play services (`play-services-auth`). On
  devices without them (Huawei), the app hides Drive from Settings → Backup.
  Local and NAS backups don't depend on Google.
