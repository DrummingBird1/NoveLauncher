# APKPure kit

APKPure is a large APK download site with its own installer app. A verified
developer listing is worth having so that the official build, not a repackaged
copy, is what people find there.

## What's here

| File | Use it for | Spec it meets |
| --- | --- | --- |
| `icon-512.png` | App icon | PNG, 512×512 |
| `feature-1024x500.png` | Banner / feature image, if the console asks for one | 1024×500 |
| `screenshots/*.png` | Screenshots | 8 images, 480×800 (APKPure's stated size; same set as `assets/screenshots/apkpure/`) |
| `listing.en.md`, `listing.he.md` | Name, short description, description, what's new | APKPure publishes no limits, so Google Play's limits are applied |
| [../PERMISSIONS.md](../PERMISSIONS.md), [../CONTENT_RATING.md](../CONTENT_RATING.md) | If the reviewers ask | |

## Account (owner's action)

1. Create a free account at [developer.apkpure.com](https://developer.apkpure.com).
2. **Claim the app** by package name, `com.ailauncher.app`, and confirm with the verification code APKPure emails you. The APKPure team approves claims by hand.

## Submitting

1. Download the signed release `.apk` from the [v10.0.0 GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases/tag/v10.0.0). APKPure takes APK/XAPK up to 2 GB.
2. In the developer console, add the app details from the listing files, the icon, the screenshots and the APK.
3. **Submit** (owner presses it). Every APK is reviewed by hand. If nothing has happened after 3 business days, email developer@apkpure.com.
4. **Updates:** submit each new version under the same app (same name and APKPure URL) so existing users get update notifications.

## Notes

- APKPure sometimes mirrors apps by itself. If a listing for
  `com.ailauncher.app` already exists, claim it rather than creating a second one.
- Users who install from APKPure get updates through APKPure's own app. The
  same APK also updates over a GitHub or Obtainium install, because the
  signature is the same.
