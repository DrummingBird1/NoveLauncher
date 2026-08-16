# Screenshots

Captured from a real emulator run (Android 36, Pixel-class device, 1080×2400)
of the signed v9.3.0 release build — not mockups.

- `raw/` — the original 1080×2400 captures. Source of truth; not directly
  uploadable to either store (see below).
- `google-play/` — 1080×2160 (cropped to exactly 2:1). Google Play requires
  screenshots between 320px and 3840px per side, and the longer side can't
  be more than **twice** the shorter side — the raw 1080×2400 captures
  violate that (2400/1080 ≈ 2.22×), so these are cropped from the top
  (removing empty space at the bottom of each screen) to land exactly at
  the 2:1 limit.
- `apkpure/` — 480×800, APKPure's stated screenshot spec. Cropped to a 0.6
  aspect ratio first (matching 480:800), then downscaled. Content-heavy
  screens (e.g. `08-security.png`) may have the last line or two of text
  cropped off at this size — that's an inherent tradeoff of fitting a
  scrolling settings screen into a fixed thumbnail ratio, not a bug.

To regenerate `google-play/`/`apkpure/` after re-capturing `raw/`, see the
crop logic (top-anchored crop to the target aspect ratio, then resize) — a
one-off Python/Pillow script, not currently checked into the repo as a
reusable tool.
