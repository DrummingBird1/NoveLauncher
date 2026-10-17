# Samsung Galaxy Store kit

Galaxy Store comes preinstalled on every Samsung phone. It reuses the release
APK from GitHub Releases as-is (same signing key), and its reviewers look hard
at launcher permissions.

## What's here

| File | Use it for | Spec it meets |
| --- | --- | --- |
| `icon-512.png` | App icon | PNG, 512×512, ≤ 1 MB |
| `screenshots/*.png` | Screenshots (upload in file-name order) | 8 images, 1080×2160 (2:1), 4–8 required, 320–3840 px per side |
| `listing.en.md`, `listing.he.md` | Title, short description, description, new features, tags, reviewer comments | Lengths checked in **UTF-8 bytes** (Samsung's unit), see each block |
| [../PERMISSIONS.md](../PERMISSIONS.md), [../CONTENT_RATING.md](../CONTENT_RATING.md) | Reviewer questions, age rating | |

Specs are from Samsung's Content Publish API reference (the same rules the
Seller Portal form enforces). There's no feature graphic for non-game apps.

## Account (owner's action)

1. Sign in to the [Seller Portal](https://seller.samsungapps.com) with a Samsung account.
2. Apply for **commercial seller status**. It's needed even for free apps. You'll provide:
   - a bank account or PayPal in the same country as your Seller Portal profile, in the account holder's own name;
   - business/identity verification: a D-U-N-S number is the easy path, otherwise DBA-type paperwork (in English); for a private seller, the representative's own name.

   Approval takes several days, and D-U-N-S or international bank checks can take up to 10 business days each.

## Submitting

1. Download the signed release `.apk` from the [v10.0.0 GitHub Release](https://github.com/DrummingBird1/NoveLauncher/releases/tag/v10.0.0). If the release notes list a SHA-256, compare it.
2. Seller Portal → Applications → **Add new application** → Android.
3. **Binary:** upload the APK. Keep the same signing key for every update.
4. **App information:** paste from `listing.en.md`, then add Hebrew from `listing.he.md` as a second language. The Hebrew description uses the shorter `full_compact` text, because the full Hebrew text is over 4000 bytes.
5. **Category:** Personalization. **Age rating:** see [CONTENT_RATING.md](../CONTENT_RATING.md).
6. **Privacy policy URL:** https://drummingbird1.github.io/NoveLauncher/privacy.html
7. **Comments to the certification team:** the English `Comments to the certification team` block.
8. **Countries:** all, unless you have a reason to limit them.
9. **Submit** (owner presses it). Certification usually takes a few days. Rejections come with a report that says exactly what to fix.

## Review risks to know before submitting

- **Update check and Rate button: handled in code.** When Galaxy Store installed the app, Settings → About hides the GitHub update check entirely, and **Rate** opens the Galaxy Store page instead of Google Play (`InstallSource`, CLAUDE.md Pitfall #40). If a reviewer still asks about update prompts, that's the answer.
- **Other stores in screenshots or text.** Screenshots must not show the Play Store icon, and the listing doesn't name Google Play. The in-app icon-pack hint ("Install … from the Play Store") is the one in-app mention.
- **The accessibility service.** Reviewers check that it's disclosed and optional. The disclosure appears as a dialog when the user double-taps the home screen, and as a card in Settings → Gestures; [../../PLAY_POLICY_NOTES.md](../../PLAY_POLICY_NOTES.md) has the full write-up.
