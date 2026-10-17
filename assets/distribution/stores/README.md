# Store submission kits

One folder per store, each holding what that store's upload form asks for:
listing text in English and Hebrew, the icon and banner sizes it wants,
screenshots cut to its size rules, plus links to the shared permission and
content-rating answers. Why these stores and not others (F-Droid, IzzyOnDroid,
Amazon) is explained in [../ALTERNATIVE_STORES.md](../ALTERNATIVE_STORES.md).

The APK is not in here, and no binary build ever goes in git. Every store gets
the signed release `.apk` from the matching
[GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases), the
same file and signature as GitHub and Obtainium installs.

| Kit | Cost | Account needs | Review |
| --- | --- | --- | --- |
| [samsung-galaxy-store/](samsung-galaxy-store/) | Free | Samsung account + commercial seller status (bank/PayPal, business or identity check) | Days, after a seller approval of days to ~2 weeks |
| [huawei-appgallery/](huawei-appgallery/) | Free | HUAWEI ID + identity verification (ID + bank card) | ~3–5 working days |
| [apkpure/](apkpure/) | Free | Developer console account; claim the package by email code | Manual, ~3 business days |
| [uptodown/](uptodown/) | Free | Developers Console account | Manual |
| [aptoide/](aptoide/) | **US$69 / €69 a year** (the app isn't on Google Play, so it's a manual submission) | Aptoide Connect account, ownership request | Manual |

Shared by every kit:

- [PERMISSIONS.md](PERMISSIONS.md): why each sensitive permission is needed.
- [CONTENT_RATING.md](CONTENT_RATING.md): age-rating and data/privacy questionnaire answers.
- Privacy policy: https://drummingbird1.github.io/NoveLauncher/privacy.html
- Support email: solvaris2@gmail.com

## Before any store: Google's Android developer verification

Since 2026-09-30, certified Android devices in Brazil, Indonesia, Singapore and
Thailand only install apps from participating stores if the developer has
verified their identity with Google and registered the package name with its
signing key. Google's
[stated plan](https://developer.android.com/developer-verification) is to
extend this to **all** installs on certified devices worldwide in 2027,
including direct APK downloads from GitHub and Obtainium. Once enforcement
reaches a user, an unregistered app can't be installed or updated there except
through Android's "advanced flow" for power users, or adb.

What the owner has to do, once:

1. Sign up for the [Android Developer Console](https://developer.android.com/developer-verification)
   with the "distribute exclusively outside Google Play" option: a **Full
   Distribution** account, US$25 one-time, plus identity verification. (A free
   Limited Distribution account only covers up to 20 devices.)
2. Register `com.ailauncher.app` with the release signing key
   (`android/release.keystore`, the key behind every GitHub Release).
   Registration needs that key, so it can never be lost.

Aptoide Connect's form already asks for this. Do it before the first store
submission rather than per store.

## Who does what

These kits are prepared in the repo. Everything that creates an account, pays,
verifies identity, accepts terms or presses **Submit** is the owner's action.
Claude can fill in a store's upload form in the browser while the owner is
signed in, but asks before every submission.

## Regenerating

Icons, banners, screenshots and the generated `listing.*.md` files all come
from one script:

```bash
uv run --with pillow python assets/distribution/stores/build_kits.py
```

- **Text:** edit [source/listing.en.md](source/listing.en.md) and
  [source/listing.he.md](source/listing.he.md), never the generated
  per-store `listing.*.md` files. The script fails if any field is over a
  store's limit. Samsung counts UTF-8 bytes, so Hebrew text uses about twice
  the space, and the script falls back to `full_compact` there.
- **Screenshots:** put 1080×2400 captures in `assets/screenshots/raw/` with
  the names listed in `SHOTS` in `build_kits.py`. The script crops each to every
  store's aspect ratio (anchored top or bottom per shot), resizes, and also
  rewrites `assets/screenshots/google-play/` and `assets/screenshots/apkpure/`.
  Stores with a lower maximum take the first N, so order matters.
- **Icon and banner:** come from `assets/graphics/` (`generate_graphics.py`).
