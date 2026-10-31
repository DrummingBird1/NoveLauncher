# Roadmap

> עברית: מפת הדרכים של NoveLauncher בקצרה (באנגלית). אין התחייבות לתאריכים.

A status list for **NoveLauncher**, a private, on-device Android launcher with Hebrew (RTL) support. It shows what has shipped, what is next and what is only an idea. There are no dates, and items can change or be dropped. Release details are in the [CHANGELOG](CHANGELOG.md).

## Shipped

- **Home screen** — apps from all profiles, work profile and Private Space, long-press shortcuts, wallpaper system (images or gradients, per page / time / day), gestures, a dock of up to five apps, widgets end to end, clock styles and fonts.
- **Search and learning** — smart Hebrew/English search with typo correction, instant answers (calculator and unit conversion), suggestions that learn from what you open, usage statistics, selectable icon packs.
- **Calendar** — Jewish calendar, holidays, candle-lighting and havdalah times, Shabbat mode.
- **Security** — app lock (PIN, password, pattern or biometric), encrypted settings, password-protected encrypted backups to local storage or a NAS, scheduled backups.
- **Languages** — seven languages with an in-app picker.
- **Engineering** — modern toolchain (target SDK 36), unit, screenshot and instrumented tests, a Baseline Profile and CI. A small APK.

## Next

- Submit to additional app stores beyond the main releases.

## Later

- Tablet screenshots and a promo video.
- Make the instrumented-test CI job blocking.
- Carry wallpaper images across devices in a settings export.
- Backup to more cloud providers, once their sign-in flows are wired.
- An on-device model for suggestions, as an upgrade to the current engine.

## Not planned

- Distribution through stores whose rules require an open-source license (F-Droid, IzzyOnDroid), for as long as the project is not open source.
- Amazon Appstore.

Ideas and bug reports are welcome as [issues](https://github.com/DrummingBird1/NoveLauncher/issues).
