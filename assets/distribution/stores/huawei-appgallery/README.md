# Huawei AppGallery kit

AppGallery is the main store on Huawei phones, which ship without Google Play
services. It reaches users in Europe, the Middle East and Africa that Google
Play doesn't. It reuses the release APK from GitHub Releases as-is.

## What's here

| File | Use it for | Spec it meets |
| --- | --- | --- |
| `icon-216.png` | App icon | PNG, 216×216, full-bleed square (AppGallery applies its own rounded mask) |
| `screenshots/*.png` | Screenshots (upload in file-name order) | 5 images, 720×1280 (9:16), 3–5 required, ≤ 2 MB each |
| `listing.en.md`, `listing.he.md` | App name, brief introduction, introduction, new features, reviewer remarks | Lengths checked in characters. **Google Drive is removed from this copy** (see below). |
| [../PERMISSIONS.md](../PERMISSIONS.md), [../CONTENT_RATING.md](../CONTENT_RATING.md) | Review questions, content rating, privacy questions | |

Specs are from AppGallery Connect's "Specifications for App Visual Assets"
page. Older guides quote 450×800 screenshots; any 9:16 image is accepted, and
720×1280 is the size the current spec names.

## Account (owner's action)

1. Create a HUAWEI ID and register at [developer.huawei.com](https://developer.huawei.com/consumer/en/).
2. Complete **identity verification**. Individual: a government ID and a bank card. Company: business registration, plus a D-U-N-S number in some regions.

## Submitting

1. Download the signed release `.apk` from the [v10.0.0 GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases/tag/v10.0.0).
2. [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html) → My apps → **New app** → Android, type **App**. The app type can't be changed later.
3. **App information:** default language English, then add Hebrew. Paste from the listing files, upload the icon and the 5 screenshots.
4. **Category:** Apps → Personalization (choose the launcher/themes sub-category the console offers).
5. **Version information:** upload the APK, and fill in the content rating questionnaire from [CONTENT_RATING.md](../CONTENT_RATING.md) (expected 3+).
6. **Privacy policy URL:** https://drummingbird1.github.io/NoveLauncher/privacy.html
7. **Distribution countries:** all **except mainland China**, which needs an ICP filing and software-copyright paperwork this project doesn't have.
8. **Remarks for the reviewer:** the English block in `listing.en.md`.
9. **Submit for review** (owner presses it). Reviews typically take 3–5 working days.

## Review risks to know before submitting

- **No Google Play services: handled in code.** Google Drive backup needs
  them, so on a phone without them Settings → Backup leaves Drive out of the
  destinations, the account card and the restore list (CLAUDE.md Pitfall #41).
  The listing copy here doesn't mention Drive either. Local and NAS backups work.
- **Update check and Rate button: handled in code.** When AppGallery
  (`com.huawei.appmarket`) installed the app, the GitHub update check is
  hidden and **Rate** opens AppGallery (CLAUDE.md Pitfall #40).
- **References to Google.** Screenshots must not show Google Play, and the
  listing doesn't mention it. The news page's web-view mode defaults to Google
  News, which some AppGallery reviewers question. Point them to the news-source
  picker if they do. Screenshot `02-search` shows the "Search Google for…" web
  search row under the instant answer. If a reviewer objects, upload only 01,
  03, 04 and 05 (AppGallery needs at least 3).
