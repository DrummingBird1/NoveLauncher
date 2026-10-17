# Uptodown kit

Uptodown is a large independent Android store, strong in Spanish-, Portuguese-
and Arabic-speaking markets. Publishing is free and self-service.

## What's here

| File | Use it for | Spec it meets |
| --- | --- | --- |
| `icon-512.png` | Icon | Square PNG, 512×512 (Uptodown rejects icons ≤ 128 px or non-square) |
| `featured-1024x500.png` | Featured image | Exactly 1024×500, banner style with the logo, as Uptodown requires |
| `screenshots/*.png` | Screenshots | 8 vertical images, 1080×1920; Uptodown asks for at least 4 |
| `listing.en.md`, `listing.he.md` | Name, short description, description, what's new | Uptodown publishes no limits, so Google Play's limits are applied |
| [../PERMISSIONS.md](../PERMISSIONS.md), [../CONTENT_RATING.md](../CONTENT_RATING.md) | If the reviewers ask | |

## Account (owner's action)

1. Sign up for the free [Uptodown Developers Console](https://en.uptodown.com/developers-console).
2. If Uptodown already lists NoveLauncher (it sometimes adds apps itself),
   **claim authorship** of that listing instead of adding a new one.

## Submitting

1. Download the signed release `.apk` from the [v10.0.0 GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases/tag/v10.0.0).
2. Developers Console → Apps → **Add new app**, then upload the APK. Files over 4 GB would need a direct-download URL, which doesn't apply here.
3. Fill in the details from the listing files, then upload the icon, the featured image and the screenshots.
4. **Publish** (owner presses it). The Alerts section shows the reason if Uptodown rejects it.
5. **Updates:** Files tab of the app → upload the new APK.

## Notes

- Uptodown's content team may rewrite the description or add its own
  screenshots for SEO. That's their policy, and you can edit it back afterwards.
- Uptodown keeps older versions downloadable. That's normal and harmless here,
  because every version is signed with the same key.
