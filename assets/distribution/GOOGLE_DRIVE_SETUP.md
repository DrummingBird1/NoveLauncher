# Google Drive backup — Google Cloud setup

NoveLauncher connects to Google Drive through Google Identity Services'
`AuthorizationClient` ([`DriveAuth.kt`](../../android/app/src/main/java/com/ailauncher/app/data/backup/DriveAuth.kt)).
Google only authorizes an Android app it can identify: a Google Cloud project
must contain an **Android OAuth client** for the package name
`com.ailauncher.app` **and** the SHA-1 fingerprint of the certificate the
installed APK was signed with. Without a matching client, "Connect Google
Drive" in Settings → Backup fails with `DEVELOPER_ERROR` (status code 10).

This is a one-time setup for the repository owner, done in the Google Cloud
console. Nothing from it goes into the repository: Android OAuth clients have
no client secret, and the app doesn't embed a client ID — Google matches the
calling app by package name and signing certificate.

The app asks for two scopes only:

| Scope | Why |
|---|---|
| `https://www.googleapis.com/auth/drive.file` | Create and see only the files NoveLauncher itself creates — never the rest of the user's Drive |
| `email` | Show which account is connected ("Connected as …") |

Both are non-sensitive scopes in Google's classification, so the app does not
need the sensitive/restricted-scope review or a security assessment.

---

## 1. Create a Google Cloud project

1. Open <https://console.cloud.google.com/> and sign in with the account that
   should own the project.
2. Project picker → **New project** → name it `NoveLauncher` → **Create**, and
   make sure it's selected.

## 2. Enable the Google Drive API

**APIs & Services → Library** → search for **Google Drive API** → **Enable**.

Without it, connecting works but every upload fails with
`Google Drive error: 403` (`accessNotConfigured`).

## 3. Configure the OAuth consent screen

**Google Auth Platform** (formerly "OAuth consent screen") → **Get started**:

- **App information:** app name `NoveLauncher`, user support email
  `solvaris2@gmail.com`.
- **Audience:** **External**.
- **Contact information:** `solvaris2@gmail.com`.

Then fill in **Branding**:

- App home page: `https://drummingbird1.github.io/NoveLauncher/`
- Privacy policy: `https://drummingbird1.github.io/NoveLauncher/privacy.html`
- Terms of service: `https://drummingbird1.github.io/NoveLauncher/terms.html`
- Leave the logo empty unless you want to go through brand verification —
  uploading one triggers it.

**Data access** → **Add or remove scopes** → add `.../auth/drive.file` and
`.../auth/userinfo.email` → **Update** → **Save**.

**Audience → Publishing status:**

- While it says **Testing**, only the Google accounts listed under **Test
  users** (up to 100) can connect, and Google may expire their authorization
  after 7 days. Add your own account there to try it out.
- For real users, press **Publish app** to move it to **In production**. With
  only non-sensitive scopes there is no scope review, but Google may still ask
  for brand verification before it shows the app's name on the consent
  screen — follow the console's prompts.

## 4. Create one Android OAuth client per signing certificate

**Google Auth Platform → Clients → Create client** → Application type
**Android**. Create one client for each certificate below, all with the
package name **`com.ailauncher.app`**. Name them so you can tell them apart.

### a. Debug builds — the committed debug key

Every debug build, local or CI, is signed with
[`android/app/debug.keystore`](../../android/app/debug.keystore)
(alias `androiddebugkey`, password `android` — a throwaway, public key; see
Pitfall #21 in `CLAUDE.md`). Its SHA-1:

```
39:10:89:44:8F:FA:25:88:B0:35:CD:E9:4D:BF:3E:F0:15:7A:31:94
```

To print it yourself (from the repository root, with a JDK on `PATH`):

```powershell
keytool -list -v -keystore android/app/debug.keystore -alias androiddebugkey -storepass android
```

Look for the `SHA1:` line under "Certificate fingerprints".

### b. Release builds — the key that signs GitHub Releases

The APKs attached to GitHub Releases are signed with the project's release
key (`CN=NoveLauncher`). Its SHA-1, read from the v9.3.0 release APK:

```
42:71:55:2E:C8:B4:69:4B:0C:4A:DC:99:E7:CC:4A:CC:61:65:84:4F
```

To read it from a published APK (no keystore password needed), download the
APK and run `apksigner` from the Android SDK build-tools:

```powershell
gh release download v9.3.0 --repo DrummingBird1/NoveLauncher --pattern "*.apk" --dir $env:TEMP\nl-apk
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.1.0\apksigner.bat" verify --print-certs "$env:TEMP\nl-apk\NoveLauncher-9.3.0-release.apk"
```

Use the `Signer #1 certificate SHA-1 digest` line. `apksigner` prints it as
lowercase hex without colons; enter it in the console as colon-separated pairs
(`AA:BB:…`), like the values above. With the keystore and its password
at hand, `keytool -list -v -keystore <release.keystore> -alias <alias>` gives
the same value.

### c. Google Play installs — the Play app signing key

If the app is published on Google Play with Play App Signing, Google re-signs
the APKs users install with **its own** app signing key, so that key needs a
client too. Play Console → your app → **App integrity** → **Play app signing**
→ copy **App signing key certificate → SHA-1 certificate fingerprint**.
(The upload key is the release key from step b, which already has a client.)

> An Android client's package name + SHA-1 pair must be unique across all of
> Google Cloud. If creating one fails because the pair is already in use, it's
> registered in another project (an old Firebase project, for example) —
> delete it there or use that project.

## 5. Test

New clients can take from a few minutes to a few hours to take effect.

1. Install a build signed with one of the registered keys (any debug build
   works).
2. Settings → Backup → **Connect Google Drive** → pick the account → allow.
   The section should then read "Connected as *your address*".
3. Run a backup with Google Drive as the destination and check that a new
   file appears in your Drive.

Known limitation (as of v10): the upload sends the backup content without
file metadata, so Drive names each file **"Untitled"** instead of
`novelauncher_backup_<date>.json`, and the app can't restore from Drive yet —
download the file and restore it as a local backup. Fixing this is a code
change in `BackupManager.backupToGoogleDrive`.

## Troubleshooting

| Symptom | Cause |
|---|---|
| "Couldn't connect to Google Drive: 10: …" (`DEVELOPER_ERROR`) | No Android client matches `com.ailauncher.app` + the SHA-1 of the APK you're running. Check the installed APK with `apksigner verify --print-certs`; for Play installs, register the Play app signing key (4c). Wait a while after creating a client. |
| Google says the app hasn't completed verification / access blocked | The consent screen is in **Testing** and the account isn't a test user — add it, or publish the app (step 3) |
| Connected, but backups fail with `Google Drive error: 403` | The Google Drive API isn't enabled in the project (step 2) |
| "Token expired. Try again or reconnect in settings." | The access token was rejected (401); the app clears it, and the next try fetches a new one. If it keeps happening, Disconnect and Connect again |
| "Not connected to Google Drive" during a scheduled backup | The user revoked access (or never connected); reconnect in Settings → Backup |
