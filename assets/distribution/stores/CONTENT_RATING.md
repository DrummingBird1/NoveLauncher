# Content-rating and privacy answers (all stores)

Each store words its age-rating and data questionnaires differently, but the
questions are the same few. Answer them from this file; the expected outcome
per store is at the bottom. These match the Google Play IARC and Data Safety
answers in [../CHECKLIST.md](../CHECKLIST.md).

## Content questions

| Question (as stores usually phrase it) | Answer | Detail if asked |
| --- | --- | --- |
| Violence, blood, gore | No | |
| Sexual content or nudity | No | |
| Profanity or crude humour | No | |
| Fear / horror themes | No | |
| Drugs, alcohol, tobacco | No | |
| Gambling or simulated gambling | No | |
| Discrimination / hate | No | |
| Users can talk to or share content with other users (chat, posts, UGC) | No | No accounts, no social features |
| App shares the user's location with other users | No | Approximate location is used on the device for weather and Shabbat times. Coordinates go only to the weather API (Open-Meteo) and are never shown to anyone. |
| Unrestricted internet access / web browsing | **Yes, limited** | The news page has two modes. RSS mode lists headlines from feeds the user picks and opens articles in the device's browser. "News site" mode shows the chosen outlet's website (Google News by default) in an in-app web view, where the user can follow links. There's no address bar or general browser. |
| Digital purchases / in-app purchases | No | Free, no IAP |
| Ads | No | |
| Collects personal information | No | Nothing reaches the developer. By Google's definition, approximate location counts as collected because it goes to the weather API; see below. |
| Designed for children | No | General audience. Not directed at children under 13. |

## Data and privacy questions

| Question | Answer |
| --- | --- |
| Does the developer collect any user data? | **Nothing reaches the developer:** no server, account, analytics or advertising ID. Forms that use Google's definition ("transmitted off the device", to anyone) should declare one type: **approximate location**. It's sent to Open-Meteo for the weather request, only if granted, processed ephemerally, not shared, for app functionality. That's the answer given on Google Play ([../CHECKLIST.md](../CHECKLIST.md)). |
| Data processed on the device | App usage and installed apps (ranking, statistics), notification counts (badges), contacts (search), photos picked as wallpapers, location for Shabbat times. None of it leaves the phone. |
| Data sent to third parties | Only when the user turns the feature on: coordinates to Open-Meteo for weather; requests to the RSS feeds the user picks; backup files to the user's own NAS; an optional daily version check to GitHub's public API (no identifiers). |
| Encrypted in transit? | Yes, HTTPS for every request. The one exception is a NAS address the user types as plain `http://` on their own network. |
| Encrypted at rest? | App-lock and backup credentials are AES-256-GCM via Android Keystore. Exported backups can optionally be password-encrypted. |
| Can users delete their data? | Yes. Everything is local, so clearing app data or uninstalling deletes it. Usage-learning can also be reset in Settings → Usage statistics. |
| Third-party SDKs that collect data | None active. The Sentry crash-report library is compiled in but has no DSN in any build, so it never starts. |
| Privacy policy | https://drummingbird1.github.io/NoveLauncher/privacy.html |

## Expected outcome per store

| Store | Form | Answer to give / expected result |
| --- | --- | --- |
| Samsung Galaxy Store | Age rating picker (All ages, 4+, 12+, 16+, 18+) | **All ages.** Samsung reviewers may raise apps that can show arbitrary web pages (the news page's in-app web view) to 12+. Accept that change rather than argue it. |
| Huawei AppGallery | Content rating questionnaire in AppGallery Connect | Answer from the tables above; expected **3+** |
| APKPure | No questionnaire at the time of writing | **Everyone** if a field appears |
| Uptodown | No questionnaire at the time of writing | **Everyone** if a field appears |
| Aptoide Connect | Age rating field + "Policy Declarations" + the Certified Developer Distribution Policy Annex (privacy section) | **Everyone**; take the privacy-annex answers from the data table above |
