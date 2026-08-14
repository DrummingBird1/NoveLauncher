# Distributing NoveLauncher outside Google Play

A practical launch plan for the other Android app stores, plus direct
distribution. Written for a privacy-focused, no-ads, no-IAP, donation-supported
launcher app (this one) — recommendations would differ for a different kind
of app.

## TL;DR priority order

1. **Samsung Galaxy Store** — easiest incremental reach, reuses your own signing key, huge install base.
2. **Direct APK via GitHub Releases** — already have the infra (CI builds artifacts every push); zero gatekeeper.
3. **Huawei AppGallery** — meaningful if you want the no-GMS Huawei audience; one real caveat (below).
4. **Amazon Appstore** — cheap to also do since it reuses the same signed build, but low expected impact for a *launcher* specifically.
5. **F-Droid** — best philosophical fit (on-device only, no tracking) but needs real engineering work first. Treat as a deliberate future project, not a same-week task.
6. **APKPure / Aptoide / similar** — not worth pursuing as a growth channel. Relevant defensively (claiming your official listing) only once the app has real traction.

---

## The one thing to get right before any of this: signing key consistency

Whichever key signs your **first** public release on a given store becomes
permanent for that store's listing — there is no "rotate the key" option
without publishing as a brand-new app and losing every review, install, and
update path.

**Do this once, before submitting anywhere:**

```bash
keytool -genkeypair -v -keystore release.keystore -alias novelauncher -keyalg RSA -keysize 2048 -validity 10000
```

Back the `.keystore` file up somewhere durable (password manager attachment +
at least one offline copy) — losing it is exactly as bad as losing the ability
to update your app. This repo already has the plumbing for it:
`android/app/build.gradle.kts` reads `RELEASE_KEYSTORE_PATH`,
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` from
environment variables (see [CLAUDE.md](../../CLAUDE.md) "Release signing"),
and CI will sign automatically once `RELEASE_KEYSTORE_BASE64` + the three
secrets are added to the GitHub repo's Actions secrets.

**Reuse that same key** for Play, Samsung, Amazon, Huawei, and direct/GitHub
distribution — they all accept a self-signed AAB/APK. **F-Droid is the one
exception**: it always builds and signs from source with its own key, so an
F-Droid install is permanently a separate signature/identity from every other
channel, whatever you do.

---

## Samsung Galaxy Store

- Register at the Samsung Developers / Seller portal (free for a standard
  developer account).
- Upload the same signed AAB/APK you'd use for Play — own key, no rebuild.
- Review is generally faster than Play's and reasonably similar in rigor.
  Expect the same scrutiny on `PACKAGE_USAGE_STATS` / `QUERY_ALL_PACKAGES` /
  notification-listener permissions — have a one-paragraph justification for
  each ready (the Play Console's "why does your app need this" answers work
  verbatim).
- Pre-installed on every Galaxy device, which is a large share of Android
  outside the US specifically — good reach for a launcher app.
- Needs its own privacy-policy URL entry and its own data-safety-style
  questionnaire (similar shape to Play's, not identical wording) — reuse the
  answers already drafted in [CHECKLIST.md](CHECKLIST.md) as a starting point.

## Huawei AppGallery

- Primary store for Huawei devices sold since the 2019 US sanctions, which
  ship without Google Play Services entirely. Meaningful reach in parts of
  Europe, the Middle East, Africa, and China specifically.
- **Real caveat, not hypothetical**: `BackupManager.kt`'s Google Drive backup
  destination uses `com.google.android.gms.auth.GoogleAuthUtil` (Google Play
  Services). On a Huawei device without GMS installed, that one backup
  destination won't work — everything else (the launcher itself, local
  backup, NAS backup) has no GMS dependency and is unaffected. Worth a quick
  pass to confirm Drive backup fails with a clean, worded error on such
  devices rather than a confusing one, but no code changes are required to
  list the app there.
- Requires a free Huawei Developer account + AppGallery Connect console.
  Accepts your own signing key.

## Amazon Appstore

- Free developer account, accepts your own signed APK — cheap to also submit
  here once you're set up for Samsung/Huawei.
- **Set expectations low for a launcher specifically**: on actual Fire OS
  devices (Fire tablets, Fire TV), Amazon's own launcher is locked in and
  third-party apps generally can't become the default home screen the way
  they can on stock/Samsung/Huawei Android. The realistic audience here is
  regular Android phone users who separately installed the Amazon Appstore
  app — a real but small slice.
- Still worth doing if time allows (near-zero marginal cost), just don't
  expect it to move the needle the way Samsung or direct distribution would.

## F-Droid

Best philosophical fit for this app — "everything runs on-device, nothing
sent to external servers" is exactly F-Droid's audience — but it's the one
channel that needs actual engineering work first, not just a submission form.

- F-Droid **builds from source itself** on its own infrastructure; you submit
  metadata (a merge request to the `fdroiddata` repo), not a pre-built APK.
  It signs with its own key — this install will never share a signature with
  any other channel.
- **The real blocker**: `play-services-auth` (used for the Google Drive
  backup destination) is a non-free Google dependency. F-Droid's inclusion
  policy would flag this with an "Anti-Feature: Non-Free Dep" tag rather than
  reject outright, but a lot of F-Droid's own user base specifically avoids
  anti-feature-tagged apps. To get a genuinely clean listing, the practical
  fix is a Gradle **product flavor** that excludes Google Drive backup (and
  the `play-services-auth` dependency) entirely for the F-Droid build,
  leaving Local/NAS backup as that flavor's only destinations. This is a
  contained, well-understood Android technique — `flavorDimensions` +
  `productFlavors { fdroid { ... }; standard { ... } }` — but it's real work, not
  a checkbox. Both flavors should keep the **same `applicationId`** (no
  `.fdroid` suffix) — a different applicationId would also require a
  per-flavor fix to `res/xml/shortcuts.xml`'s hardcoded
  `android:targetPackage` (see Pitfall #15 in [CLAUDE.md](../../CLAUDE.md)),
  which same-applicationId flavors sidestep entirely. Concretely, the
  Google-Drive-specific code that would need to move behind a flavor-specific
  interface (same package + class name, one implementation per
  `src/fdroid/java/...` and `src/standard/java/...` source set — the standard
  Android pattern for flavor-swappable implementations) lives in exactly two
  places:
  - [android/app/src/main/java/com/ailauncher/app/data/backup/BackupManager.kt](../../android/app/src/main/java/com/ailauncher/app/data/backup/BackupManager.kt) —
    `backupToGoogleDrive()`, `fetchDriveAccessToken()`, `isGoogleConnected()`,
    `getConnectedEmail()` (all the `GoogleAuthUtil`/Drive REST calls).
  - [android/app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt](../../android/app/src/main/java/com/ailauncher/app/ui/LauncherActivity.kt) —
    `initiateGoogleSignIn()`, the `googleSignInLauncher` `ActivityResultLauncher`,
    and the `GoogleSignIn`/`GoogleSignInOptions` imports.
  The `fdroid` flavor's versions of both would make `BackupDestination.GOOGLE_DRIVE`
  behave like `ONEDRIVE`/`BOX` already do today — return a clear
  "not available in this build" `BackupResult.Error` — and the Settings UI's
  Google Drive card would need the same kind of availability filter
  `BackupSection` already applies to OneDrive/Box.
- `QUERY_ALL_PACKAGES` and `PACKAGE_USAGE_STATS` will also draw reviewer
  questions — both are legitimately justified for a launcher and should be
  fine with a clear explanation, just expect back-and-forth.
- Timeline: F-Droid is volunteer-run; inclusion review can take weeks to
  months depending on backlog. Don't block a launch on it.
- **Recommendation**: treat as a separate, deliberate future project — do the
  flavor split first, then submit, rather than trying to fold it into the
  same push as everything else.

## Direct APK distribution (GitHub Releases)

- Zero gatekeeper, full control, your own signing key. `.github/workflows/release.yml`
  (added v9.3) already does this: push a `vX.Y.Z` tag and it builds the release
  APK + AAB and opens a **draft** GitHub Release with both attached — signed
  automatically once the keystore secrets described above are added to the
  repo, unsigned (with an explicit CI warning) otherwise. It's deliberately
  left as a draft rather than auto-published, so a human reviews the release
  notes and artifacts before anyone can download them.
- **Real friction to expect**: users must enable "install unknown apps" for
  whatever app they downloaded the APK with, and Android shows a genuinely
  scary-looking warning first. This meaningfully hurts conversion for
  non-technical users — direct APK works best as a channel for power users,
  beta testers, and people who already trust you (e.g., from this GitHub
  repo), not as a general-audience channel.
- **No automatic updates** unless you build an update-check mechanism
  yourself (e.g., an in-app check against the GitHub Releases API prompting
  the user to download the new APK). Worth calling out explicitly in the
  release notes/README so users aren't surprised they need to manually
  reinstall for updates.
- **Publish a SHA-256 checksum** alongside every release APK so technically-
  inclined users can verify integrity before installing — cheap trust signal,
  one line in the release notes (`sha256sum app-release.apk`).
- Good secondary use: an internal/beta channel for testing a release before
  pushing it to Play/Samsung/Huawei, independent of whether you pursue direct
  distribution as a real ongoing channel.

## APKPure / Aptoide / similar third-party repositories

- These aren't developer-controlled the way an app store submission is — both
  platforms have historically mirrored apps by scraping other stores or
  accepting community uploads, sometimes without the developer's direct
  involvement. Don't be surprised if the app shows up there on its own once
  it's live elsewhere.
- Both do offer an official developer/verified-publisher upload path if you
  want to claim your own listing. The main reason to bother is **defensive**:
  an official, verified listing makes it harder for a malicious repackaged
  clone to impersonate you convincingly. It's not a meaningful growth channel
  on its own.
- **Recommendation**: not worth proactive effort now. Revisit only if you
  notice an unofficial/suspicious listing for the app gaining traction and
  want to displace it with an official one.

---

## Cross-cutting notes that apply to every store

- **Privacy policy + terms of service** need a real, stable public HTTPS URL
  for every single store, not just Play. The repo already has
  `PRIVACY_POLICY.html` and a hosting walkthrough in
  [PRIVACY_URL_GUIDE.md](PRIVACY_URL_GUIDE.md) — host it once, reuse the same
  URL everywhere.
- **Data-safety-style questionnaires** differ in exact wording per store
  (Play's "Data Safety" form, Samsung/Huawei/Amazon's own equivalents) but
  ask fundamentally the same things. The answers already drafted in
  [CHECKLIST.md](CHECKLIST.md) (nothing collected off-device, location only
  for the weather widget, no ads, no IAP) are a solid template to adapt per
  store rather than rewrite from scratch.
- **Support email** (`solvaris2@gmail.com`) needs to stay consistent and
  actually monitored across every listing — reviewers at multiple stores do
  test that the contact address works before approving.
- **versionCode/versionName** don't need to diverge across stores — each
  store tracks its own install base independently, so submitting the same
  build with the same version numbers everywhere is normal and expected.
- **Localization as a targeting signal**: the app now ships in seven
  languages (he/en/ar/fr/ru/es/de). Worth weighing when prioritizing — e.g.
  Huawei AppGallery's strength in Arabic-speaking and European markets, or
  Samsung's global reach, line up naturally with the existing locale list.
- **Store listing assets**: `assets/graphics/` and `assets/screenshots/`
  cover Play's requirements; other stores mostly accept the same PNGs with
  only minor resizing — check each store's exact dimension requirements
  before upload rather than assuming byte-for-byte reuse.
- **Use staged/internal testing where a store offers it** (Play has internal
  testing tracks, Samsung/Huawei have staged rollout options) before a full
  public release — especially relevant here given the sensitive permission
  set (usage stats, notification access, query-all-packages) a launcher
  legitimately needs but that any reviewer will look at twice.
