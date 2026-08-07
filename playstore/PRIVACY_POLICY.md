# Privacy Policy — NoveLauncher

**Effective date:** April 25, 2026
**Last updated:** April 25, 2026

NoveLauncher ("we", "our", or "the app") is operated as a personal-use Android launcher application. This privacy policy explains what data the app accesses, how it is used, and your rights as a user.

## TL;DR

**NoveLauncher does not collect, transmit, or store any personal data on external servers.** Everything happens on your device.

## 1. Information We Access (Locally Only)

To provide its functionality, NoveLauncher reads the following on your device:

### 1.1 App Usage Statistics
- **What:** Which apps you open, when, and for how long
- **Permission:** `PACKAGE_USAGE_STATS`
- **Why:** To rank your most-used apps and predict what you'll need next
- **Where it goes:** Stored only in a local SQLite database on your device
- **Sharing:** Never transmitted anywhere

### 1.2 Installed Apps List
- **What:** Names, package IDs, and icons of installed apps
- **Permission:** `QUERY_ALL_PACKAGES`
- **Why:** To display them in the launcher
- **Sharing:** Never transmitted

### 1.3 Notifications (Optional)
- **What:** Source app of incoming notifications (NOT the content)
- **Permission:** `BIND_NOTIFICATION_LISTENER_SERVICE` (you must enable this manually)
- **Why:** To show unread badges on app icons
- **Sharing:** Never transmitted

### 1.4 Location (Optional)
- **What:** Approximate location for weather widget
- **Permission:** `ACCESS_COARSE_LOCATION`
- **Why:** To show local weather only
- **Sharing:** Sent only to Open-Meteo (https://open-meteo.com), which does not log requests
- **Opt out:** Don't grant the permission. Weather will fall back to Tel Aviv as default.

### 1.5 Contacts (Optional)
- **What:** Contact names and phone numbers
- **Permission:** `READ_CONTACTS`
- **Why:** To include contacts in unified search results
- **Sharing:** Never transmitted

### 1.6 Biometric Authentication (Optional)
- **What:** Fingerprint or face authentication results (only "success/failure", not biometric data itself)
- **Permission:** `USE_BIOMETRIC`
- **Why:** To unlock locked apps and the launcher
- **Sharing:** Handled entirely by Android system, never accessed by us

## 2. Optional Cloud Backup

If you choose to back up your settings to a cloud service:

- **Google Drive / OneDrive / Box:** You sign in with your own account. We only have access to upload files to a single folder you choose. Your settings file is uploaded as-is (JSON format).
- **NAS:** You provide your own server address. The file is sent to that address only.
- **Local file:** Saved to your device's Downloads folder. Never transmitted.

We do not have access to your cloud account credentials. OAuth tokens are stored locally in `EncryptedSharedPreferences` on your device.

## 3. Third-Party Services

The only network requests NoveLauncher makes are:

| Service | URL | Purpose | Data Sent |
|---|---|---|---|
| Open-Meteo | api.open-meteo.com | Weather | Latitude/longitude only |
| RSS news feeds | Various publishers | News page | None |
| Google Drive (if you opt in) | googleapis.com | Backup | Your settings JSON |
| Google Sign-In (if you opt in) | accounts.google.com | Drive auth | Your Google account |

We do not include any analytics SDKs, advertising SDKs, crash reporters, or tracking libraries.

## 4. Data Retention

- All data is stored locally on your device
- You can clear all data at any time via Android Settings → Apps → NoveLauncher → Storage → Clear Storage
- Uninstalling the app removes everything

## 5. Children's Privacy

NoveLauncher does not target children under 13. We do not knowingly collect any data from children.

## 6. Your Rights (GDPR / CCPA)

Since we do not collect or store any data on external servers, there is nothing for us to provide, delete, or correct on your behalf. All data lives on your device and is under your control.

## 7. Changes to This Policy

If this policy changes, we will update the "Last updated" date at the top. Material changes will be highlighted in the app's release notes.

## 8. Contact

Questions or concerns? Email us at:
**solvaris2@gmail.com**

---

By using NoveLauncher, you confirm that you have read and understood this privacy policy.
