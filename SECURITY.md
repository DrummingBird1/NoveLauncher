# Security Policy

## Reporting a vulnerability

Please report security issues **privately** to **solvaris2@gmail.com**, or
through GitHub's
[private vulnerability reporting](https://github.com/DrummingBird1/NoveLauncher/security/advisories/new).
Don't open a public issue for a security problem.

Include what you found, how to reproduce it, and the app version. You'll get
an acknowledgement as quickly as possible — this is a small project, so
please allow a reasonable window before disclosing publicly.

## Supported versions

Only the latest release receives fixes.

| Version | Supported |
|---------|-----------|
| 9.3.x   | ✅ |
| < 9.3   | ❌ |

## What this app's security model actually promises

Being precise here matters more than sounding impressive:

**App lock, the private folder and hidden apps are a deterrent, not a
sandbox.** They intercept launches that go through NoveLauncher. A "locked"
app is still reachable from recents, notifications, another launcher, ADB,
app links or the assistant. This is inherent to every third-party launcher —
no launcher can provide OS-level isolation. For real isolation, use Android's
built-in Work Profile.

Reports that a locked app can be opened by one of those routes are therefore
**expected behaviour**, not vulnerabilities.

## What is genuinely protected

- **Credentials**: PINs, passwords and patterns are hashed with
  PBKDF2-HMAC-SHA256 using a per-install salt stored in
  EncryptedSharedPreferences. Legacy unsalted values are migrated on first
  successful verification.
- **Brute force**: failed attempts and an exponential lockout are persisted,
  so force-stopping the app does not reset them.
- **At rest**: security and backup settings are encrypted with AES-256-GCM
  via the Android Keystore before being written to disk.
- **Portable backups**: exports can be wrapped in PBKDF2-derived AES-256-GCM
  so they can be restored on another device without exposing their contents.
  The NAS password is always stripped from exports.
- **Auto-backup exclusions**: credential storage and the local database are
  excluded from Android's cloud auto-backup.
- **Transport**: NAS/WebDAV backups are HTTPS-only and refuse plaintext
  endpoints.

Findings that weaken any of the above **are** in scope, and are appreciated.
