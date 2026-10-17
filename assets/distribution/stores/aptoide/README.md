# Aptoide kit

Aptoide is an independent store. Apps submitted through **Aptoide Connect**
also go to Aptoide's partner stores (OEM and carrier stores such as GetApps and
OPPO App Market).

**Cost: this one isn't free for NoveLauncher.** Aptoide Connect distributes
for free only apps that are already on Google Play and have no in-app
purchases. Everything else is a manual submission, and Aptoide only reviews it
with an active **Aptoide Connect subscription: US$69 / €69 a year**
(auto-renews, refundable within 30 days if no app has started distributing).
Decide whether that's worth it before doing anything else here.

## What's here

| File | Use it for | Spec it meets |
| --- | --- | --- |
| `icon-512.png` | Icon | 512×512, JPG/PNG, ≤ 5 MB |
| `feature-1024x500.png` | Feature graphic | 1024×500, ≤ 5 MB |
| `screenshots/*.png` | Screenshots (per language) | 8 images, 1080×2160; Aptoide allows 320–3840 px, max 2:1, ≤ 5 MB |
| `listing.en.md`, `listing.he.md` | Title, description, release notes | Aptoide publishes no limits, so Google Play's limits are applied |
| [../PERMISSIONS.md](../PERMISSIONS.md), [../CONTENT_RATING.md](../CONTENT_RATING.md) | Policy declarations, privacy annex | |

## Account (owner's action)

1. Create a developer account at [connect.aptoide.com](https://connect.aptoide.com) and start the subscription (see the cost note above).
2. The submission form includes **Android developer verification** (Google's requirement, see [../README.md](../README.md)) and an **app ownership request** for `com.ailauncher.app`. Aptoide reviews the ownership request by hand.

## Submitting

1. Download the signed release `.apk` from the [v10.0.0 GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases/tag/v10.0.0). Aptoide takes APKs only, not `.aab`.
2. Fill in the submission form: app files, distribution countries, distribution channels (partner stores), then app information and media from this kit. NoveLauncher isn't on Google Play, so Aptoide can't fetch the listing automatically.
3. Policy declarations and the Certified Developer Distribution Policy Annex: answers are in [CONTENT_RATING.md](../CONTENT_RATING.md).
4. Choose the release mode (immediate or manual), then **Submit** (owner presses it).
5. Listing changes after this need a new version or an update request.

## Free alternative

Anyone can create a free personal store on Aptoide and upload an APK to it.
That listing isn't verified as the developer's and doesn't reach the partner
stores, so it does little more than the GitHub Release already does.
