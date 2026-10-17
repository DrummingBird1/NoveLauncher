"""
Builds the per-store submission kits in this folder from three sources:

  assets/graphics/playstore-icon-1024.png, feature-graphic-1024x500.png
  assets/screenshots/raw/*.png              (1080x2400 emulator captures)
  assets/distribution/stores/source/listing.{en,he}.md

and also refreshes the two derived screenshot sets the repo already keeps,
assets/screenshots/google-play/ (1080x2160) and assets/screenshots/apkpure/
(480x800), so every resized copy comes from one crop function.

Run from anywhere:   uv run --with pillow python assets/distribution/stores/build_kits.py

The script fails (exit 1) when a listing field is over a store's limit, a
store would get fewer screenshots than it requires, or an image is over a
store's file-size cap. Everything it writes is overwritten on each run.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.dirname(os.path.dirname(HERE))
GRAPHICS = os.path.join(ASSETS, "graphics")
SHOTS_DIR = os.path.join(ASSETS, "screenshots")
RAW = os.path.join(SHOTS_DIR, "raw")
SOURCE = os.path.join(HERE, "source")

PRIVACY_URL = "https://drummingbird1.github.io/NoveLauncher/privacy.html"
TERMS_URL = "https://drummingbird1.github.io/NoveLauncher/terms.html"
WEBSITE = "https://drummingbird1.github.io/NoveLauncher/"
SUPPORT_EMAIL = "solvaris2@gmail.com"
SOURCE_URL = "https://github.com/DrummingBird1/NoveLauncher"

# Background of the master icon just inside its rounded corners — used to fill
# the corners for stores that want a full-bleed square (they mask it themselves).
ICON_BG = (20, 20, 42)

# Screenshot order, and where to anchor the crop when a store's aspect ratio is
# shorter than the 1080x2400 capture: 0.0 keeps the top, 1.0 keeps the bottom.
# Stores with a lower maximum take the first N, so the strongest shots go first.
# Names that don't exist in raw/ are skipped.
SHOTS: list[tuple[str, float]] = [
    ("01-home", 0.0),
    ("02-search", 0.0),
    ("03-shortcuts", 1.0),
    ("04-apps-profiles", 0.8),
    ("05-wallpaper", 0.0),
    ("06-calendar", 0.0),
    ("07-themes", 0.0),
    ("08-security", 0.0),
    ("09-settings", 0.0),
]

# Text the Huawei build must not carry: AppGallery devices ship without Google
# Play Services, so Google Drive backup can't work there. In 10.0.0 Drive is
# switched off everywhere (BuildConfig.DRIVE_BACKUP_ENABLED), so the source copy
# doesn't mention it and every store is checked; keep this map for when Drive
# returns, because Huawei must still drop it then.
NO_GMS = {
    "en": [("to your phone, Google Drive or your own NAS", "to your phone or your own NAS")],
    "he": [("לטלפון, ל-Google Drive או ל-NAS ביתי", "לטלפון או ל-NAS ביתי")],
}

PLAY_STYLE = "no published limit; Google Play's limit used"

STORES = {
    "samsung-galaxy-store": {
        "name": "Samsung Galaxy Store",
        # Content Publish API reference: lengths are UTF-8 bytes.
        "fields": [
            ("App title", "title", 100, "bytes", ""),
            ("Short description", "short_tiny", 40, "bytes", ""),
            ("Description", "full", 4000, "bytes", ""),
            ("New features", "whats_new", 4000, "bytes", ""),
            ("Tags", "keywords", None, None, ""),
            ("Comments to the certification team", "reviewer_note", 4000, "bytes", "English only"),
        ],
        "category": "Personalization (the Content Publish API calls it \"Decoration\")",
        "rating": "All ages (0+)",
        "icons": [("icon-512.png", 512, "rounded", 1024)],
        "feature": None,
        "screens": {"dir": "screenshots", "size": (1080, 2160), "min": 4, "max": 8, "max_kb": 1024},
        "no_gms": False,
    },
    "huawei-appgallery": {
        "name": "Huawei AppGallery",
        "fields": [
            ("App name", "title", 64, "chars", ""),
            ("Brief introduction", "short", 80, "chars", ""),
            ("Introduction", "full", 8000, "chars", ""),
            ("New features", "whats_new", 500, "chars", "conservative; the console may allow more"),
            ("Keywords", "keywords", None, None, ""),
            ("Remarks for the reviewer", "reviewer_note", None, None, "English only"),
        ],
        "category": "Apps > Personalization (pick the launcher / themes sub-category the console offers)",
        "rating": "3+",
        "icons": [("icon-216.png", 216, "square", 500)],
        "feature": None,
        "screens": {"dir": "screenshots", "size": (720, 1280), "min": 3, "max": 5, "max_kb": 2048},
        "no_gms": True,
    },
    "apkpure": {
        "name": "APKPure",
        "fields": [
            ("App name", "title", 50, "chars", PLAY_STYLE),
            ("Short description", "short", 80, "chars", PLAY_STYLE),
            ("Description", "full", 4000, "chars", PLAY_STYLE),
            ("What's new", "whats_new", 500, "chars", PLAY_STYLE),
        ],
        "category": "Personalization",
        "rating": "Everyone",
        "icons": [("icon-512.png", 512, "rounded", 1024)],
        "feature": "feature-1024x500.png",
        "screens": {"dir": "screenshots", "size": (480, 800), "min": 2, "max": 8, "max_kb": 1024},
        "no_gms": False,
    },
    "uptodown": {
        "name": "Uptodown",
        "fields": [
            ("Name", "title", 50, "chars", PLAY_STYLE),
            ("Short description", "short", 80, "chars", PLAY_STYLE),
            ("Description", "full", 4000, "chars", PLAY_STYLE),
            ("What's new", "whats_new", 500, "chars", PLAY_STYLE),
        ],
        "category": "Customization (Uptodown's name for Personalization)",
        "rating": "Everyone",
        "icons": [("icon-512.png", 512, "rounded", 1024)],
        "feature": "featured-1024x500.png",
        "screens": {"dir": "screenshots", "size": (1080, 1920), "min": 4, "max": 8, "max_kb": 2048},
        "no_gms": False,
    },
    "aptoide": {
        "name": "Aptoide (Aptoide Connect)",
        "fields": [
            ("Title", "title", 50, "chars", PLAY_STYLE),
            ("Description", "full", 4000, "chars", PLAY_STYLE),
            ("Release notes (news)", "whats_new", 500, "chars", PLAY_STYLE),
        ],
        "category": "Personalization",
        "rating": "Everyone",
        "icons": [("icon-512.png", 512, "rounded", 5120)],
        "feature": "feature-1024x500.png",
        "screens": {"dir": "screenshots", "size": (1080, 2160), "min": 2, "max": 8, "max_kb": 5120},
        "no_gms": False,
    },
}

# The repo's long-standing derived screenshot sets (see assets/screenshots/README.md).
REPO_SETS = [("google-play", (1080, 2160)), ("apkpure", (480, 800))]

errors: list[str] = []


def parse_listing(lang: str) -> dict[str, str]:
    text = open(os.path.join(SOURCE, f"listing.{lang}.md"), encoding="utf-8").read()
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    parts = re.split(r"^## (\w+)\s*$", text, flags=re.M)
    return {parts[i]: parts[i + 1].strip() for i in range(1, len(parts), 2)}


def measure(value: str, unit: str | None) -> int:
    return len(value.encode("utf-8")) if unit == "bytes" else len(value)


def pick(listing: dict[str, str], key: str, limit: int | None, unit: str | None) -> tuple[str, str]:
    """Returns (key actually used, text). Falls back to <key>_compact when over the limit."""
    for k in (key, f"{key}_compact"):
        if k in listing and (limit is None or measure(listing[k], unit) <= limit):
            return k, listing[k]
    return key, listing.get(key, "")


def crop_to(img: Image.Image, size: tuple[int, int], anchor: float) -> Image.Image:
    """Crop to the target aspect ratio (keeping full width or height), then resize."""
    w, h = img.size
    tw, th = size
    if w / h > tw / th:  # source wider than target: trim the sides, centred
        cw = round(h * tw / th)
        box = ((w - cw) // 2, 0, (w - cw) // 2 + cw, h)
    else:  # source taller than target: trim top/bottom around the anchor
        ch = round(w * th / tw)
        y0 = round((h - ch) * anchor)
        box = (0, y0, w, y0 + ch)
    return img.crop(box).resize(size, Image.LANCZOS).convert("RGB")


def raw_shots(raw_dir: str) -> list[tuple[str, float, Image.Image]]:
    out = []
    for name, anchor in SHOTS:
        path = os.path.join(raw_dir, f"{name}.png")
        if os.path.exists(path):
            out.append((name, anchor, Image.open(path).convert("RGB")))
    return out


def reset_dir(path: str) -> None:
    if os.path.isdir(path):
        shutil.rmtree(path)
    os.makedirs(path)


def check_kb(path: str, max_kb: int, store: str) -> None:
    kb = os.path.getsize(path) / 1024
    if kb > max_kb:
        errors.append(f"{store}: {os.path.relpath(path, HERE)} is {kb:.0f} KB (max {max_kb} KB)")


def make_icon(size: int, shape: str) -> Image.Image:
    master = Image.open(os.path.join(GRAPHICS, "playstore-icon-1024.png")).convert("RGBA")
    if shape == "square":
        bg = Image.new("RGBA", master.size, ICON_BG + (255,))
        master = Image.alpha_composite(bg, master).convert("RGB")
    return master.resize((size, size), Image.LANCZOS)


def write_listing(slug: str, cfg: dict, lang: str, listing: dict[str, str]) -> None:
    lang_name = {"en": "English", "he": "Hebrew (עברית)"}[lang]
    lines = [
        f"# {cfg['name']} listing: {lang_name}",
        "",
        "<!-- Generated by ../build_kits.py from ../source/listing.{lang}.md. Edit the source, not this file. -->".replace("{lang}", lang),
        "",
        "Paste each block into the field of the same name. Lengths were checked",
        "when this file was generated.",
        "",
    ]
    for label, key, limit, unit, note in cfg["fields"]:
        if key == "reviewer_note" and lang != "en":
            continue
        used, value = pick(listing, key, limit, unit)
        if not value:
            errors.append(f"{slug}/{lang}: missing '{key}' in source/listing.{lang}.md")
            continue
        if cfg["no_gms"]:
            for old, new in NO_GMS[lang]:
                if old in value:
                    value = value.replace(old, new)
        if "Google Drive" in value:
            errors.append(f"{slug}/{lang}: '{label}' still mentions Google Drive")
        size = measure(value, unit)
        if limit is not None and size > limit:
            errors.append(f"{slug}/{lang}: '{label}' is {size} {unit} (max {limit})")
        meta = f"{size} {unit or 'chars'}" + (f" of {limit} max" if limit else "")
        if used != key:
            meta += f", shortened version (`{used}`)"
        if note:
            meta += f"; {note}"
        lines += [f"## {label}", "", f"_{meta}_", "", "```text", value, "```", ""]

    lines += [
        "## Other fields",
        "",
        f"- **Category:** {cfg['category']}",
        f"- **Age / content rating:** {cfg['rating']}",
        "- **Price:** Free, no in-app purchases, no ads",
        f"- **Privacy policy URL:** {PRIVACY_URL}",
        f"- **Terms of service URL:** {TERMS_URL}",
        f"- **Website:** {WEBSITE}",
        f"- **Support email:** {SUPPORT_EMAIL}",
        f"- **Source code (for reviewers):** {SOURCE_URL}",
        "- **Content-rating and privacy answers:** [CONTENT_RATING.md](../CONTENT_RATING.md)",
        "- **Permission justifications:** [PERMISSIONS.md](../PERMISSIONS.md)",
        "",
    ]
    with open(os.path.join(HERE, slug, f"listing.{lang}.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


def build_store(slug: str, cfg: dict, shots, listings) -> None:
    root = os.path.join(HERE, slug)
    os.makedirs(root, exist_ok=True)

    for fname, size, shape, max_kb in cfg["icons"]:
        path = os.path.join(root, fname)
        make_icon(size, shape).save(path, "PNG", optimize=True)
        check_kb(path, max_kb, slug)

    if cfg["feature"]:
        shutil.copyfile(os.path.join(GRAPHICS, "feature-graphic-1024x500.png"), os.path.join(root, cfg["feature"]))

    sc = cfg["screens"]
    out_dir = os.path.join(root, sc["dir"])
    reset_dir(out_dir)
    chosen = shots[: sc["max"]]
    if len(chosen) < sc["min"]:
        errors.append(f"{slug}: only {len(chosen)} screenshots, needs at least {sc['min']}")
    for i, (name, anchor, img) in enumerate(chosen, 1):
        path = os.path.join(out_dir, f"{name}.png")
        crop_to(img, sc["size"], anchor).save(path, "PNG", optimize=True)
        check_kb(path, sc["max_kb"], slug)

    for lang, listing in listings.items():
        write_listing(slug, cfg, lang, listing)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--raw", default=RAW, help="folder of 1080x2400 captures (default: assets/screenshots/raw)")
    ap.add_argument("--kits-only", action="store_true", help="don't rewrite assets/screenshots/google-play and apkpure")
    args = ap.parse_args()

    shots = raw_shots(args.raw)
    if not shots:
        print(f"No screenshots matching SHOTS found in {args.raw}", file=sys.stderr)
        return 1
    listings = {lang: parse_listing(lang) for lang in ("en", "he")}

    for name, size in [] if args.kits_only else REPO_SETS:
        out_dir = os.path.join(SHOTS_DIR, name)
        reset_dir(out_dir)
        for shot, anchor, img in shots:
            crop_to(img, size, anchor).save(os.path.join(out_dir, f"{shot}.png"), "PNG", optimize=True)

    for slug, cfg in STORES.items():
        build_store(slug, cfg, shots, listings)

    print(f"{len(shots)} screenshots from raw/: " + ", ".join(s[0] for s in shots))
    if errors:
        print("\nProblems:", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        return 1
    print("All kits built; every length and size limit holds.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
