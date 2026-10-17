# Distributing NoveLauncher outside Google Play

Where NoveLauncher can be published besides Google Play, what each channel
takes, and which are closed to it. Facts as of **2026-10-17**. Store rules
change, so re-check the linked sources before acting on an old row.

Upload-ready material for each store (listing text in English and Hebrew,
icons, banners, screenshots, permission and rating answers) is in
[stores/](stores/README.md).

## Summary

| Channel | Status | What it takes |
| --- | --- | --- |
| **GitHub Releases** | Live. Since v9.3.0 every release carries a signed release APK. | Nothing new: push a `vX.Y.Z` tag ([release.yml](../../.github/workflows/release.yml)) |
| **Obtainium** | Live, no submission needed | Nothing: it installs and updates straight from GitHub Releases. Badge on the README and website. |
| **Samsung Galaxy Store** | Kit ready: [stores/samsung-galaxy-store](stores/samsung-galaxy-store/README.md) | Owner: Samsung account + commercial seller status, then submit |
| **Huawei AppGallery** | Kit ready: [stores/huawei-appgallery](stores/huawei-appgallery/README.md) | Owner: HUAWEI ID + identity verification, then submit. Google Drive backup doesn't work there. |
| **Uptodown** | Kit ready: [stores/uptodown](stores/uptodown/README.md) | Owner: free developer account, then submit |
| **APKPure** | Kit ready: [stores/apkpure](stores/apkpure/README.md) | Owner: free developer account, claim the package, then submit |
| **Aptoide** | Kit ready: [stores/aptoide](stores/aptoide/README.md) | Owner: Aptoide Connect subscription (**US$69 / €69 a year**), then submit |
| **F-Droid** | **Not possible** | Needs an open-source license (see below) |
| **IzzyOnDroid** | **Not possible** | Needs an open-source license (see below) |
| **Amazon Appstore** | **Gone** | Discontinued on non-Amazon Android devices on 2025-08-20 |

Suggested order for the owner: Android developer verification first (below),
then Samsung (biggest reach, slowest account approval), Huawei, Uptodown and
APKPure. Aptoide only if US$69 a year seems worth it.

---

## Applies to every channel

### Google's Android developer verification

Since **2026-09-30**, certified Android devices in Brazil, Indonesia, Singapore
and Thailand only install apps from participating stores if the developer has
verified their identity with Google and registered the app's package name with
its signing key. Google's plan is to extend this to all installs on certified
devices worldwide in **2027**, including direct APK downloads (GitHub,
Obtainium). Once enforcement reaches a user, an unregistered app can only be
installed or updated through Android's "advanced flow" for power users, or adb.

The owner should do this once, well before 2027: open an
[Android Developer Console](https://developer.android.com/developer-verification)
**Full Distribution** account (US$25 one-time, identity verification), then
register `com.ailauncher.app` with the release signing key. Details are in
[stores/README.md](stores/README.md#before-any-store-googles-android-developer-verification).

### One signing key everywhere

The release key was created on 2026-08-16 and lives in
`android/release.keystore`, outside git. **Never generate another one.** Every
store listing, and Google's developer verification, is tied to the key that
signs the first upload, and a different key can't update anything already
installed. Back the file up in at least two places (a password-manager
attachment plus an offline copy). CI signs with it through the
`RELEASE_KEYSTORE_BASE64` / `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_ALIAS` /
`RELEASE_KEY_PASSWORD` Actions secrets (see [CLAUDE.md](../../CLAUDE.md),
"Release signing").

Because every channel ships the same signed APK, a user can move between
GitHub, Obtainium, Samsung, Huawei, Uptodown, APKPure and Aptoide, and updates
still install over each other.

### Shared listing facts

- Privacy policy: https://drummingbird1.github.io/NoveLauncher/privacy.html
  (served from `docs/privacy.html`). Terms: `.../terms.html`.
- Support email: `solvaris2@gmail.com`. Reviewers do write to it, so keep it monitored.
- Same `versionCode`/`versionName` in every store. Each store tracks its own installs.
- Permission justifications: [stores/PERMISSIONS.md](stores/PERMISSIONS.md), with
  longer ready-to-paste text in [PLAY_POLICY_NOTES.md](PLAY_POLICY_NOTES.md).
  Rating and data questionnaires: [stores/CONTENT_RATING.md](stores/CONTENT_RATING.md).
  Only approximate location leaves the device (to the weather API, if granted).
- No store accepts listings or screenshots that mention or show another store.
  The kits' screenshots and text follow that rule.
- The [LICENSE](../../LICENSE) doesn't let anyone except the owner publish the
  app to a store. An unofficial mirror is a license violation the owner can
  report, and claiming the listing (APKPure, Uptodown) is the practical fix.

---

## GitHub Releases

Pushing a `vX.Y.Z` tag runs [release.yml](../../.github/workflows/release.yml).
It builds the signed release APK and opens a **draft** GitHub Release that a
person reviews before publishing. The workflow deliberately
fails if the signing secrets are missing, so an unsigned APK can never be
attached. Since v9.3.0 each release has a signed release `.apk`. The `.aab`
that 9.3.0 and 10.0.0 also carried was removed on 2026-10-17: it is only an
upload format for Google Play, which isn't in use. v9.1.0 and v9.2.0 only had debug APKs, signed with a
different key, so installs from those two can't update in place. Add the APK's
SHA-256 to the release notes by hand; the workflow doesn't yet.

Expect friction: users have to allow "install unknown apps", and Android
warns them first. Updates don't arrive by themselves. The in-app update
check (Settings → About, opt-in, once a day) shows a link to the newest
release, and Obtainium automates the whole thing. Store installs never see
that check: the app hides it when a store is the installer of record, and its
Rate button opens that store (CLAUDE.md Pitfall #40).

## Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) installs apps straight from
their release pages and keeps them updated. It needs no submission, account or
approval. Users add `https://github.com/DrummingBird1/NoveLauncher` as an app
and get every new GitHub Release as an update, signed with the same key.

The README (all seven languages) and the website show the official "Get it on
Obtainium" badge (a self-hosted copy in `docs/assets/`, as Obtainium asks)
with that one-line instruction. The badge links to Obtainium's GitHub page,
not to Obtainium's `apps.obtainium.imranr.dev/redirect` helper, because that
page loads ads and analytics. The website also offers a direct
`obtainium://add/…` link for people who already have Obtainium. GitHub strips
custom-scheme links from READMEs, so those can't have one. Each release
attaches exactly one `.apk` (next to a card image), so Obtainium
picks it without any filter.

## Samsung Galaxy Store

Preinstalled on every Samsung phone, and it takes the release APK unchanged.
The owner needs commercial seller status even for a free app: a bank account
or PayPal, plus business or identity checks that can take up to two weeks.
Reviewers look hard at launcher permissions. Kit, steps and review risks:
[stores/samsung-galaxy-store](stores/samsung-galaxy-store/README.md).

## Huawei AppGallery

The store on Huawei phones, which ship without Google Play services. It
reaches Europe, the Middle East and Africa. Google Drive backup needs Google
Play services, so on those phones the app hides it and the AppGallery copy
doesn't mention it; local and NAS backups work. Exclude mainland China, which needs an ICP filing.
Kit: [stores/huawei-appgallery](stores/huawei-appgallery/README.md).

## Uptodown, APKPure, Aptoide

All three accept the release APK as-is:

- **Uptodown:** free, self-service, strong in Spanish-, Portuguese- and
  Arabic-speaking markets. [Kit](stores/uptodown/README.md).
- **APKPure:** free. Mostly worth doing defensively, so the official build is
  what people find. [Kit](stores/apkpure/README.md).
- **Aptoide Connect:** reaches Aptoide's partner OEM and carrier stores, but
  it's free only for apps already on Google Play. NoveLauncher isn't, so a
  submission needs the **US$69 / €69 a year** subscription.
  [Kit](stores/aptoide/README.md).

## F-Droid and IzzyOnDroid: not possible

Both only distribute free and open-source software:

- [F-Droid's inclusion policy](https://f-droid.org/docs/Inclusion_Policy/)
  requires an OSI- or FSF-approved license and builds every app from source.
- [IzzyOnDroid's inclusion policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/)
  also requires an OSI/FSF license, even though it ships the developer's own APK.

NoveLauncher's [LICENSE](../../LICENSE) is "All Rights Reserved" (source
available, no redistribution). On **2026-10-17 the owner decided to keep it**,
so neither repository can list the app. Relicensing released code can't be
undone, which is why it's the owner's decision alone.

For the record, if that ever changes, the work would be:

1. Relicense: a new `LICENSE` and an announcement. The git history has only
   the owner's and Claude's commits, so there are no outside contributors to ask.
2. A `foss` product flavor without `play-services-auth` (F-Droid rejects
   non-free dependencies). The Google-specific code is
   [data/backup/DriveAuth.kt](../../android/app/src/main/java/com/ailauncher/app/data/backup/DriveAuth.kt),
   the Drive branch of
   [data/backup/BackupManager.kt](../../android/app/src/main/java/com/ailauncher/app/data/backup/BackupManager.kt),
   and the Connect/Disconnect control in
   [ui/screens/BackupSettings.kt](../../android/app/src/main/java/com/ailauncher/app/ui/screens/BackupSettings.kt).
   In the `foss` flavor, Google Drive would be filtered out of the destination
   list the way OneDrive and Box already are. Keep the same `applicationId`
   so `res/xml/shortcuts.xml` stays valid (CLAUDE.md, Pitfall #15).
3. Fastlane metadata (`fastlane/metadata/android/<locale>/`) for IzzyOnDroid,
   plus an `fdroiddata` merge request on GitLab, opened by the owner.

## Amazon Appstore: gone

Amazon discontinued the Appstore on non-Amazon Android devices on
**2025-08-20**. It now exists only on Fire tablets and Fire TV, where Amazon's
own launcher is locked in and a third-party home screen can't be made the
default.
