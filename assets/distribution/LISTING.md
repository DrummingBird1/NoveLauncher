# NoveLauncher — Google Play Store Listing (Hebrew-first)

The Hebrew-first variant of the listing. The full bilingual copy, release
notes and store URLs live in [STORE_LISTING.md](STORE_LISTING.md); keep the
two in agreement.

## App Name
NoveLauncher - AI Smart Launcher

## Short Description (80 chars)
לאנצ'ר חכם לאנדרואיד — AI שמארגן לך את הטלפון לפי ההרגלים שלך

## Full Description (4000 chars max)

🚀 NoveLauncher — הלאנצ'ר החכם שלומד אותך

NoveLauncher הוא לאנצ'ר אנדרואיד פרטי שמארגן את מסך הבית לפי הדרך שבה אתה באמת משתמש בטלפון. הוא לומד את הרגלי השימוש שלך על המכשיר ומציב את האפליקציות הנכונות בזמן הנכון — בלי חשבון ובלי הגדרות.

✨ מה מיוחד ב-NoveLauncher?

🧠 AI מקומי — דירוג לפי שעה ביום, תדירות, שימוש אחרון וקטגוריה, ו"מומלצות עכשיו" לומדת מאילו הצעות אתה באמת פותח. הכל מתבצע על המכשיר — בלי שליחת נתוני שימוש לשום שרת.

📂 תיקיות חכמות — אפליקציות מתמיינות אוטומטית ל-11 קטגוריות: עבודה, חברה, פנאי, משחקים, קניות ועוד. לא צריך לסדר ידנית.

💼 פרופיל עבודה ומרחב פרטי — אפליקציות עבודה עם התג שלהן ומתג השהיה; המרחב הפרטי של אנדרואיד 15 באזור נפרד שאפשר לנעול.

⚡ קיצורי דרך — לחיצה ארוכה על אפליקציה פותחת את קיצורי הדרך שלה.

🔍 חיפוש חכם — עברית ואנגלית, סבלנות לשגיאות הקלדה וראשי תיבות, מחשבון והמרת יחידות.

🕯️ לוח עברי ומצב שבת — תאריך עברי, חגים, זמני הדלקת נרות וצאת שבת שמחושבים בטלפון; מצב שבת מסתיר הצעות מסיחות ותגי התראות.

🖼️ רקעים — תמונות משלך או מעברי צבע: קבוע, לפי עמוד, לפי שעה, לפי יום, או רקע חי. בהירות, ניגודיות, רוויה, חמימות, טשטוש וויניאטה.

👆 מחוות — החלקה למעלה לאפליקציות, למטה להתראות, הקשה כפולה לכיבוי המסך.

🎨 עיצוב מלא — 12 ערכות נושא, Material You, צבעים מותאמים, 16 גופנים, 12 צורות אייקונים, חבילות אייקונים, עיצוב שעון ותצוגה מקדימה בזמן אמת.

📰 עמוד חדשות — RSS מובנה עם 12 מקורות (ynet, Google News, BBC, CNN ועוד) ופידים משלך.

🔒 אבטחה מתקדמת — נעילת אפליקציות ב-PIN, סיסמה, דפוס, טביעת אצבע או זיהוי פנים. תיקייה פרטית מוסתרת. נעילת פריסה של אייקונים.

💾 גיבוי — מקומי או לשרת NAS פרטי, עם תזמון והצפנה אופציונלית בסיסמה.

🧩 ווידג'טים — הוסף כל ווידג'ט מהמערכת, הגדר אותו ושנה את גודלו.

📱 עמודים מותאמים — צור עמודים חדשים, שנה סדר, הפעל או כבה עמודים.

👁️ פרטיות — בלי חשבון, בלי אנליטיקס, בלי פרסומות. הקוד גלוי לעיון ב-GitHub.

⚡ חסכוני בסוללה — רענון כל 15 דקות ורק כשמסך הבית מוצג, ממשק Jetpack Compose יעיל.

## Category
Personalization

## Tags
launcher, home screen, AI, smart, customization, themes, privacy, hebrew,
work profile, private space, jewish calendar

---

# מדיניות פרטיות / Privacy Policy — summary

The full, binding policy is published at
<https://drummingbird1.github.io/NoveLauncher/privacy.html> (source:
[`docs/privacy.html`](../../docs/privacy.html)). This is a summary for store
forms that ask for one.

### Data Collection
NoveLauncher does NOT collect personal data and has no servers of its own.

- App usage statistics are read from Android's UsageStatsManager and processed on the device
- App categorization uses on-device heuristics (no cloud AI)
- Settings are stored locally using Android DataStore; security and backup settings are encrypted
- Backups go only to destinations YOU choose (local file or your own NAS) and are encrypted only if you set a backup password

### Network connections
- Weather: Open-Meteo, with approximate coordinates only if location permission is granted (a default city otherwise)
- News: the RSS feeds enabled on the News page, fetched directly from each publisher
- Backups (opt-in): your own NAS over HTTPS
- Update check (opt-in, off by default): GitHub's releases API, at most once a day
- Nothing else. No analytics, tracking or advertising SDKs.

### Permissions Explained
- PACKAGE_USAGE_STATS: to rank apps from your usage and show the Statistics page
- QUERY_ALL_PACKAGES: to show every installed app, as a launcher must
- ACCESS_HIDDEN_PROFILES: to show Android 15's Private Space in its own section
- BIND_NOTIFICATION_LISTENER_SERVICE (optional): unread badges — counts only, never content
- ACCESS_COARSE_LOCATION (optional): local weather and Jewish-calendar times
- READ_CONTACTS (optional): contacts in search results, on the device only
- USE_BIOMETRIC: fingerprint/face app lock, handled by Android
- INTERNET: weather, news feeds, the backups you set up and the optional update check
- EXPAND_STATUS_BAR: swipe down on the home screen to open notifications
- Accessibility service (optional, off by default): screen off on double-tap and open notifications — it cannot read screen content

### Contact
solvaris2@gmail.com

---

# תנאי שימוש / Terms of Service — summary

The full terms are published at
<https://drummingbird1.github.io/NoveLauncher/terms.html> (source:
[`docs/terms.html`](../../docs/terms.html)).

1. ACCEPTANCE: By using NoveLauncher, you agree to these terms.

2. LICENSE: NoveLauncher grants you a personal, non-exclusive, non-transferable, revocable licence to use the app. The source code is published for transparency and review under the repository's LICENSE — it is not an open-source licence.

3. PRIVACY: We do not collect personal data. See the Privacy Policy for details.

4. AS-IS: The app is provided "as is" without warranties of any kind. We are not responsible for data loss, device issues, or any damages arising from use of the app. Jewish-calendar times are a convenience, not a religious authority.

5. MODIFICATIONS: We reserve the right to update the app and these terms. Continued use after changes constitutes acceptance.

6. INTELLECTUAL PROPERTY: NoveLauncher and its design are protected by copyright. You may not sell, sublicense or redistribute the app.

7. TERMINATION: We may terminate access to the app at any time for violation of these terms.

8. GOVERNING LAW: These terms are governed by the laws of Israel.

9. CONTACT: solvaris2@gmail.com
