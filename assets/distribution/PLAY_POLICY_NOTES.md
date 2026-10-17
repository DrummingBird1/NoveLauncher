# Play Console — Permission and API Declarations

Ready-to-paste answers for the sensitive permissions and APIs NoveLauncher
uses, written against the code as of v10. Each section says where the text
goes, then gives it in a quote block you can copy as-is.

Two of these have dedicated Play Console forms (the **AccessibilityService
API** and **QUERY_ALL_PACKAGES**). The others have no form of their own at the
time of writing (October 2026), but reviewers ask about them — keep the text
ready for review questions, the release notes to the reviewer, or a reply to
a policy email.

If the app ever starts using one of these differently, the declaration has to
be updated before the release that changes it — Play treats an out-of-date
declaration as an undeclared use.

| What | Code | Play Console |
|---|---|---|
| AccessibilityService | `ui/gestures/LockScreenAccessibilityService.kt`, `res/xml/lock_screen_accessibility.xml` | App content → **Accessibility API** declaration (+ video) |
| `QUERY_ALL_PACKAGES` | `AndroidManifest.xml` | App content → **Permissions declaration** (package visibility) |
| `PACKAGE_USAGE_STATS` | `data/UsageStatsRepository.kt` | No form — review notes |
| Notification listener | `data/LauncherNotificationListener.kt` | No form — review notes |
| `ACCESS_HIDDEN_PROFILES` | `data/InstalledAppsRepository.kt` | No form — review notes |
| `EXPAND_STATUS_BAR` | `ui/gestures/GestureActions.kt` | Normal permission, nothing to declare |

---

## 1. AccessibilityService API — `LockScreenAccessibilityService`

**Is it an accessibility tool?** **No.** The service is declared with
`android:isAccessibilityTool="false"`. Play lists launchers among the
non-tool uses, which means the in-app prominent disclosure, affirmative
consent and a video are all required.

**What it does (for your own reference and for any free-text field):**

> NoveLauncher is a home-screen launcher. Its optional "NoveLauncher gestures"
> accessibility service performs exactly two global actions, and only when the
> user makes the matching gesture on the launcher's home screen:
> double-tap → turn the screen off (`GLOBAL_ACTION_LOCK_SCREEN`), and
> swipe down → open the notification shade (`GLOBAL_ACTION_NOTIFICATIONS`).
> The service is declared with `canRetrieveWindowContent="false"`, subscribes
> only to `typeAnnouncement` events and ignores them, and never reads, stores
> or transmits any data. It is off by default; the user enables it in Android's
> Accessibility settings after an in-app explanation, and every other launcher
> feature works without it.
>
> No narrower API does this. `DevicePolicyManager.lockNow()` would need
> device-admin rights — a far broader grant — and forces PIN entry instead of
> allowing biometric unlock. There is no public API for opening the
> notification shade; without the service the launcher falls back to the
> status-bar API under the normal `EXPAND_STATUS_BAR` permission, which some
> manufacturers block.

**Why is the API used?** Select **App functionality** only.

**Is any user data collected or shared through the API?** **No.**

**Prominent disclosure (in-app).** The disclosure text is the
`gesture_lock_setup_body` string. English (`values-en`):

> To turn the screen off, NoveLauncher uses an optional Android accessibility
> service. It performs only two actions — screen off and opening the
> notification shade — and only when you make the gesture. It cannot read
> what's on your screen, see what you type, or collect any data. You can turn
> it off any time in Accessibility settings.

Hebrew (`values/`, the app's default locale):

> כדי לכבות את המסך, NoveLauncher משתמש בשירות נגישות אופציונלי של Android.
> השירות מבצע רק שתי פעולות — כיבוי המסך ופתיחת מגירת ההתראות — ורק כשאתם
> מבצעים את המחווה. הוא לא יכול לקרוא את מה שמוצג על המסך, לראות מה אתם
> מקלידים או לאסוף מידע כלשהו. אפשר לכבות אותו בכל עת בהגדרות הנגישות.

The same string exists in all seven locales. It appears in two places:

- **On the home screen** — `LockGestureSetupDialog` in `ui/LauncherRoot.kt`.
  When double-tap-to-lock is switched on but the service isn't enabled, a
  double-tap opens this dialog: title "Double tap to lock", the disclosure,
  and two buttons — **Open accessibility settings** (consent: it opens the
  system screen where the user turns the service on) and **Cancel** (no
  consent: the dialog closes and nothing changes). This is the
  disclosure to show in the video, because it appears during normal use.
- **Settings → Gestures** — a card with the same title and text, the current
  status ("Accessibility helper: on/off") and the same button.

Play's rules for this disclosure, and how the app meets them:

| Requirement | NoveLauncher |
|---|---|
| Inside the app, not only in the listing or privacy policy | In-app dialog and Settings card |
| Shown during normal use | The home-screen dialog appears on the gesture itself |
| Says what data is accessed and how it is used | Two actions, no screen content, no typing, no data collected |
| Not bundled with other disclosures | A dedicated dialog for this service only |
| Affirmative consent | The user taps **Open accessibility settings** and then turns the service on in Android's own settings |
| Not replaced by the manifest description | The manifest's `a11y_service_description` repeats it, but the dialog is the disclosure |

One weak spot to know about: the double-tap switch itself is in Settings →
Gestures (off by default), so the first time a user meets the feature may be
in Settings rather than on the home screen. The dialog still appears on the
home screen before the service can be enabled from there. If a reviewer
objects, the fix is in code (e.g. showing the dialog right when the switch is
turned on), not in this text.

**Video (required).** Upload an unlisted YouTube video (portrait, ~1 minute)
showing, in order, scrolling slowly enough that every line of the disclosure
can be read:

1. Opening NoveLauncher on the home screen.
2. Settings → Gestures: the explanation card, then turning on
   **Double tap → screen off**.
3. Back on the home screen: double-tap → the disclosure dialog → **Cancel**
   (non-consent path: nothing happens).
4. Double-tap again → **Open accessibility settings** → enabling
   "NoveLauncher gestures" in Android's settings (show the system
   confirmation).
5. Back on the home screen: double-tap turns the screen off; swipe down opens
   the notifications.

On Android 13+ a sideloaded build shows the service as a *restricted setting*
until it's allowed from App info; a Play-installed build doesn't, so record
the video with a build installed from the Play internal testing track.

---

## 2. `QUERY_ALL_PACKAGES`

Play allows this permission when the app's core purpose needs to see any or
all installed apps; its listed examples include device search. Choose the
category closest to **device search / launcher** that the form offers, and
paste:

> NoveLauncher is a home-screen launcher (it declares the HOME intent category
> and is meant to be set as the default home app). Its core purpose is to
> display, search, organise and launch the apps installed on the device, so it
> must see every installed app:
>
> - the app grid, smart folders and on-device ranking cover every launchable
>   app, across the personal, work and Private Space profiles;
> - global search finds any installed app by name;
> - the Usage Statistics page shows screen time for every app the user used,
>   including apps without a launcher icon, so it needs their labels and
>   icons;
> - app lock and hidden apps let the user pick any installed app;
> - icon-pack support discovers installed icon packs from several launcher
>   conventions (Nova, ADW, Lawnchair).
>
> The list of installed apps never leaves the device: it isn't uploaded,
> shared, or used for advertising or analytics.

Be ready for a reviewer to ask whether a narrower method would do — the app
grid itself comes from `LauncherApps`; the Statistics page, app pickers and
icon-pack discovery are what need full visibility.

---

## 3. `PACKAGE_USAGE_STATS` (Usage access)

No separate form — the user grants it in Android's special-access settings.
Text for the reviewer:

> NoveLauncher uses Usage access (PACKAGE_USAGE_STATS) for its core feature:
> ordering the home screen by how the user actually uses their apps. It reads
> which apps were opened, when and for how long, and uses that on the device
> to rank suggestions ("Recommended now"), to group apps, and to show the
> Usage Statistics page (screen time by day, week or month). The data is
> processed only on the device and never transmitted. The onboarding screen
> explains why it's needed before sending the user to Android's Usage access
> settings, and the user can decline; the launcher still works without it,
> just without usage-based ranking or statistics.

---

## 4. Notification listener (`BIND_NOTIFICATION_LISTENER_SERVICE`)

No separate form. Text for the reviewer:

> NoveLauncher's notification listener exists only to show unread badges on
> app icons. For each active notification it reads the posting app's package
> name to count notifications per app; it does not read or store titles, text
> or any other notification content, and ongoing (persistent) notifications
> are skipped. The counts (package name, number of unread notifications, time
> of the latest one) are kept in the launcher's local database and never
> transmitted.
> The user enables notification access manually in Android settings; badges
> can be switched off or snoozed, and the launcher works fully without it.

---

## 5. `ACCESS_HIDDEN_PROFILES` (Private Space, Android 15+)

No separate form at the time of writing. Android grants this permission only
to the app that holds the home role (the default launcher). Text for the
reviewer:

> NoveLauncher declares ACCESS_HIDDEN_PROFILES so that, when the user has set
> it as their default home app on Android 15 or later, it can show the apps in
> the user's Private Space — the same thing the system launcher does. Private
> Space apps are kept in their own section with Lock/Unlock controls; while
> the space is locked the launcher shows none of its apps, and they are never
> used for suggestions, smart folders or the main app grid. Nothing about
> Private Space apps leaves the device. On older Android versions, or while
> NoveLauncher isn't the default home app, the permission has no effect.

---

## 6. `EXPAND_STATUS_BAR`

A normal permission, granted at install; nothing to declare. It backs the
swipe-down-for-notifications gesture when the accessibility service is off.

---

## Related

- Data safety answers: [CHECKLIST.md](CHECKLIST.md), section "Data safety form answers"
- Privacy policy (lists the accessibility service, Private Space and every
  network connection): <https://drummingbird1.github.io/NoveLauncher/privacy.html>
- Google Drive OAuth setup: [GOOGLE_DRIVE_SETUP.md](GOOGLE_DRIVE_SETUP.md)
