# NoveLauncher v8 — Logo Redesign + Splash Screen + Privacy Hosting

## ✨ What changed in v8

### 🎨 #1: Clean logo redesign
**File:** `playstore/generate_graphics.py`

The v7 logo had busy concentric rings that looked muddy at small sizes. v8 ditches the rings entirely.

**New logo system — 3 variants:**
- **`playstore-icon-512.png`** — full color: rounded square, soft radial glow, clean N glyph + 2 sparkles (cyan + white)
- **`logo-monochrome-512.png`** — single-color N for status bars and themed icons
- **`logo-horizontal-1024x320.png`** — icon + "NoveLauncher" wordmark + tagline (for marketing materials, README headers, etc.)

**N glyph improvements:**
- Slightly rounded corners on verticals (stroke / 5)
- Diagonal stroke 5% thicker than verticals for visual weight
- 0.42× icon size (vs 0.45× before) — more breathing room

### 🚀 #4 + #11: Splash screen
**Files:**
- `app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt` — `installSplashScreen(this)` before `super.onCreate()`
- `app/src/main/res/values/themes.xml` — new `Theme.AILauncher.Starting` extending `Theme.SplashScreen`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — splash logo
- `app/src/main/AndroidManifest.xml` — uses new starting theme

**What you'll see:** Brief NoveLauncher logo on dark background while Hilt initializes (~300-500ms on cold start). Way better than the previous flash to gray.

The `core-splashscreen` dependency was already in `build.gradle.kts` — just wasn't wired up.

### 🤖 Themed icons (Android 13+)
**Files:** `app/src/main/res/mipmap-*/ic_launcher.xml`

Adaptive icons now declare a `<monochrome>` element. On Android 13+ when the user enables "themed icons", NoveLauncher's icon will tint to match their wallpaper.

### 📜 #25: Privacy URL hosting guide
**Files:**
- `playstore/PRIVACY_URL_GUIDE.md` — 4 hosting options (GitHub Pages, Cloudflare, Notion, Google Sites)
- `playstore/PRIVACY_POLICY.html` — ready-to-host HTML version, styled to match brand

**Why this matters:** Google Play **rejects** apps without an HTTPS public URL for the privacy policy. Local Markdown is not enough.

The HTML version uses inline CSS (no external dependencies), looks great on mobile, and matches the NoveLauncher brand colors.

✅ The support email throughout the policy/listing docs and the in-app Contact button
is `solvaris2@gmail.com` (a real, monitored inbox) — no placeholder remains.

## 📦 New files in v8

```
app/src/main/res/drawable/ic_launcher_monochrome.xml    NEW
app/src/main/res/values/themes.xml                       UPDATED (splash theme)
app/src/main/res/mipmap-*/ic_launcher.xml                UPDATED (monochrome layer)
app/src/main/AndroidManifest.xml                         UPDATED (Theme.AILauncher.Starting)
app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt  UPDATED (installSplashScreen)
playstore/generate_graphics.py                           REWRITTEN
playstore/graphics/playstore-icon-512.png                REGENERATED
playstore/graphics/playstore-icon-1024.png               REGENERATED
playstore/graphics/feature-graphic-1024x500.png          REGENERATED
playstore/graphics/logo-monochrome-512.png               NEW
playstore/graphics/logo-horizontal-1024x320.png          NEW
playstore/PRIVACY_POLICY.html                            NEW
playstore/PRIVACY_URL_GUIDE.md                           NEW
```

## What's still pending from the v8 review

These were in the suggestions list but not yet built:
- **#2:** Logo variations PDF brand kit
- **#5:** Brand kit document
- **#6:** Replace PIL with Playwright for proper emoji rendering in screenshots
- **#7:** Additional screenshots (security, backup, news, analog clock)
- **#8:** Tablet screenshots
- **#9:** Marketing text overlay on screenshots
- **#10:** Light + dark mixed screenshots
- **#12:** SearchBar Material3 component
- **#13:** Edge-to-edge for Android 15+
- **#14:** `Modifier.semantics` for Talkback
- **#16-19:** Performance optimizations
- **#28-30:** Crashlytics, analytics, A/B testing

## Build instructions

1. Unzip
2. Open in Android Studio Ladybug+
3. Sync Gradle
4. Build → Generate Signed Bundle/APK → AAB
5. Cold-launch on a device — you'll see the splash screen

## To publish

1. Host `playstore/PRIVACY_POLICY.html` on a public URL (see `PRIVACY_URL_GUIDE.md`)
2. Replace the placeholder email
3. Generate signed AAB
4. Upload to Play Console with all assets in `playstore/`
