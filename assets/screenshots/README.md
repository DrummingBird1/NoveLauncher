# Screenshots

Captured on 2026-10-17 from a real emulator run (AVD `Medium_Phone_API_36.1`,
Android 16, 1080×2400) of a v10 debug build of `main`, English UI. They're not
mockups. A debug build looks identical to the release: same app ID, label and
UI. The emulator had a test work profile and a Private Space so the Apps page
shows both, and Play Store plus a few system utilities were hidden in
NoveLauncher's own Hidden apps so no store or developer tool shows up (Samsung
and Huawei reject screenshots that show another store).

| File | Shows |
| --- | --- |
| `01-home` | Home with a built-in gradient wallpaper, Shabbat mode's greeting, the Hebrew date, Havdalah time, weather and "Recommended now" |
| `02-search` | Search with an instant answer (`10 km to mi`) |
| `03-shortcuts` | Long-press sheet: the app's own shortcuts plus hide / lock / App info |
| `04-apps-profiles` | Apps page with All / Personal / Work filters, badged work apps and the lockable Private Space section |
| `05-wallpaper` | Wallpaper modes (single, per page, hourly, daily, live) and an active wallpaper's controls |
| `06-calendar` | Jewish calendar settings: today's Hebrew date, next holiday, Shabbat times, Shabbat mode |
| `07-themes` | Display mode, Material You and the 12 theme presets |
| `08-security` | App lock methods and the "deterrent, not a sandbox" note |
| `09-settings` | The Settings index |

- `raw/`: the original 1080×2400 captures. Source of truth, not directly
  uploadable (Google Play's longer side can't exceed twice the shorter side,
  and 2400/1080 ≈ 2.22).
- `google-play/`: 1080×2160 (exactly 2:1). Also used by the README and,
  as copies in `docs/assets/screenshots/`, by the website.
- `apkpure/`: 480×800 (0.6 aspect ratio), APKPure's stated size.

Both derived sets, and every other store's sizes under
[../distribution/stores/](../distribution/stores/README.md), come from one
script. Each capture is cropped to the target aspect ratio and then resized:

```bash
uv run --with pillow python assets/distribution/stores/build_kits.py
```

The crop keeps the full width and trims top or bottom around a per-shot
anchor set in `SHOTS` in that script. `03-shortcuts` is anchored to the bottom
(the sheet), `04-apps-profiles` at 0.8 (so the filter chips and Private Space
both survive), and the rest to the top. To re-shoot, replace the files in
`raw/` with the same names, then run the script. Stores with a lower maximum
(Huawei takes 5) use the first N, so keep the strongest shots first.
