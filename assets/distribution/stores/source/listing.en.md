<!--
Master English store copy for v10. build_kits.py splits this file on its
"## key" headings and writes each store's listing.en.md, checking the store's
length limits. Edit here, then re-run the script; don't edit the generated files.
Keys used: title, short, short_tiny, full, full_compact (optional), whats_new,
keywords, reviewer_note (English only).
-->

## title
NoveLauncher - Smart AI Launcher

## short
A private home screen that learns your habits, entirely on your phone

## short_tiny
Smart launcher that learns your habits

## full
NoveLauncher is an Android home screen that learns which apps you need and puts them in front of you. It doesn't send anything about you anywhere: every suggestion is worked out on your phone.

WHAT'S INSIDE

• Suggestions that learn: "Recommended now" ranks your apps by time of day, how often and how recently you use them, and which suggestions you actually tap. When headphones are connected, your music apps move up.
• Smart search: find apps, contacts and settings despite a typo, by initials, or by typing a name in Hebrew letters for an English name (and the other way round). Type 15% of 80 or 10 km to mi for an instant answer.
• Hebrew calendar and Shabbat: the Hebrew date, upcoming holidays, and candle-lighting and Havdalah times for where you are. An optional Shabbat mode quiets social, game and shopping suggestions and notification badges until nightfall. Nothing is blocked.
• Work profile and Private Space: work apps and Android 15's Private Space get their own section. Pause work apps or lock the space right from the launcher.
• App shortcuts: long-press any icon for its shortcuts, App info, hide, lock or uninstall.
• Wallpapers: your own photos or built-in gradients, with a different one per page, per hour or per weekday. Adjust brightness, blur and warmth, or show your live wallpaper.
• Gestures: swipe up for all apps, swipe down for notifications, double-tap to turn the screen off (optional).
• Smart folders: apps are sorted automatically into Work, Social, Games, Tools and more.
• Make it yours: 12 themes, custom colours or colours from your wallpaper, 16 fonts, icon shapes, third-party icon packs, a digital, analog or minimal clock, widgets, custom pages and a dock.
• App lock: a PIN, password, pattern or fingerprint per app, a hidden private folder, and hidden apps.
• News page: a built-in RSS reader with Hebrew and English sources, or add your own feeds.
• Usage statistics: screen time and opens per app, by day, week or month.
• Backups: to your phone or your own NAS, on a schedule, optionally encrypted with a password only you know.
• Seven languages: Hebrew, English, Arabic, French, Russian, Spanish and German, with full right-to-left support.

PRIVACY

No ads, no analytics, no account. Your usage data never leaves your phone. The app only goes online for things you turn on: weather (Open-Meteo), news feeds, backups you set up, and an optional once-a-day check for a new version.

ABOUT APP LOCK

App lock keeps people who borrow your phone out of your apps. It is not a security guarantee: like every third-party launcher, it can't stop an app from opening from a notification, the recent-apps screen or another launcher.

PERMISSIONS

• Usage access: to rank your apps. The data stays on your phone.
• Notification access (optional): for unread badges.
• Approximate location (optional): for weather and Shabbat times.
• Contacts (optional): to show contacts in search.
• Accessibility service (optional): used only for double-tap to lock and swipe down for notifications. It cannot read your screen.

## whats_new
Version 10:
• Work profile and Private Space support, plus long-press app shortcuts
• Wallpapers: your own photos or built-in gradients, per page, hour or weekday
• Smart search with typo tolerance, Hebrew-English matching and instant answers
• Hebrew calendar, holidays, Shabbat times and an optional Shabbat mode
• Suggestions that learn from what you tap
• Icon packs, a full widget flow, home-screen gestures and real usage statistics

## keywords
launcher, home screen, smart launcher, privacy, no ads, Hebrew, Jewish calendar, Shabbat, RTL, icon packs, widgets, app lock, smart folders, wallpapers

## reviewer_note
NoveLauncher is a home-screen (launcher) app. To test it, install it, open it and finish the short welcome screens, then set it as the default Home app when asked (or in Settings > Apps > Default apps > Home app). No account or login is needed, and there are no ads or in-app purchases.

Why it asks for each sensitive permission:
- Usage access (PACKAGE_USAGE_STATS): ranks apps in "Recommended now" and feeds the Usage statistics page. Processed only on the device.
- QUERY_ALL_PACKAGES: a launcher has to list every installed app in its app drawer and search.
- Notification access (optional): counts unread notifications to show badges on app icons. Notification content is not stored or sent.
- Approximate location (optional): local weather from Open-Meteo, and sunset-based candle-lighting times.
- Contacts (optional): contacts appear in search results. Nothing leaves the device.
- Accessibility service (optional, off until the user turns it on): performs exactly two global actions when the user makes the gesture, turning the screen off (double tap) and opening the notification shade (swipe down). It never retrieves window content (canRetrieveWindowContent="false"), and the app shows a full disclosure screen before sending the user to Accessibility settings.
- ACCESS_HIDDEN_PROFILES: shows Android 15 Private Space apps in their own section when NoveLauncher is the default home app.
- EXPAND_STATUS_BAR: swipe down on the home screen opens the notification shade.
- Biometric: optional fingerprint unlock for apps the user locked.
- Post notifications: only to report a scheduled backup that failed.
