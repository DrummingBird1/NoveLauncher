"""Builds the fonts bundled in android/app/src/main/res/font/ from Google Fonts.

Usage (from the repo root):
    uv run --no-project --with fonttools==4.60.1 python -I assets/fonts/prepare_fonts.py <download-dir>

Every font is SIL OFL 1.1 (texts in assets/fonts/licenses/). The source files come
from github.com/google/fonts at a pinned commit. Each font except Varela Round is
then made smaller: axes other than weight are pinned (width 100, slant 0), the
glyph set is cut to the scripts the launcher's 7 languages and typical app names
use, and TrueType hinting is dropped. The weight axis stays variable, so Light,
Regular, Medium and Bold all render as real weights (see launcherFontFamily in
ui/theme/Theme.kt). Varela Round ships unmodified: its OFL reserves the name
"Varela Round", and a modified copy would have to be renamed.

The name table (copyright, OFL licence and URL) is kept in every file, and the
combined licence text is written to the APK's assets/licenses/fonts-OFL.txt.
"""

import io
import os
import sys
import urllib.request

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

GOOGLE_FONTS_COMMIT = "45e3d5269e4a17d823f6d84470bc084dc5ae46f6"  # google/fonts main, 2026-10-21
BASE = f"https://raw.githubusercontent.com/google/fonts/{GOOGLE_FONTS_COMMIT}/ofl"

# res/font name -> (google/fonts directory, file name)
FONTS = {
    "heebo": ("heebo", "Heebo[wght].ttf"),
    "rubik": ("rubik", "Rubik[wght].ttf"),
    "assistant": ("assistant", "Assistant[wght].ttf"),
    "varela_round": ("varelaround", "VarelaRound-Regular.ttf"),
    "open_sans": ("opensans", "OpenSans[wdth,wght].ttf"),
    "montserrat": ("montserrat", "Montserrat[wght].ttf"),
    "poppins_light": ("poppins", "Poppins-Light.ttf"),
    "poppins_regular": ("poppins", "Poppins-Regular.ttf"),
    "poppins_bold": ("poppins", "Poppins-Bold.ttf"),
    "cairo": ("cairo", "Cairo[slnt,wght].ttf"),
    "frank_ruhl_libre": ("frankruhllibre", "FrankRuhlLibre[wght].ttf"),
    "secular_one": ("secularone", "SecularOne-Regular.ttf"),
    "noto_sans": ("notosans", "NotoSans[wdth,wght].ttf"),
}
UNMODIFIED = {"varela_round"}

UNICODE_RANGES = [
    (0x0020, 0x007E), (0x00A0, 0x024F),  # Latin, Latin-1, Latin Extended-A/B
    (0x0300, 0x036F),  # combining marks
    (0x0370, 0x03FF),  # Greek
    (0x0400, 0x052F),  # Cyrillic + supplement
    (0x0590, 0x05FF), (0xFB1D, 0xFB4F),  # Hebrew + presentation forms
    (0x0600, 0x06FF), (0x0750, 0x077F), (0x08A0, 0x08FF),  # Arabic
    (0xFB50, 0xFDFF), (0xFE70, 0xFEFF),  # Arabic presentation forms
    (0x1E9E, 0x1E9E),  # capital sharp s
    (0x2000, 0x206F), (0x2070, 0x209F), (0x20A0, 0x20CF),  # punctuation, super/subscripts, currency (₪)
    (0x2100, 0x214F), (0x2150, 0x218F), (0x2190, 0x21FF), (0x2200, 0x22FF),
    (0x25CC, 0x25CC), (0xFFFD, 0xFFFD),
]

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
RES_FONT = os.path.join(REPO, "android", "app", "src", "main", "res", "font")
LICENSES = os.path.join(HERE, "licenses")
APK_LICENSE = os.path.join(REPO, "android", "app", "src", "main", "assets", "licenses", "fonts-OFL.txt")


def fetch(url: str, path: str) -> None:
    if not os.path.exists(path):
        with urllib.request.urlopen(url) as r, open(path, "wb") as f:
            f.write(r.read())


def shrink(path: str) -> TTFont:
    font = TTFont(path)
    if "fvar" in font:
        pins = {
            a.axisTag: (100 if a.axisTag == "wdth" else 0 if a.axisTag == "slnt" else a.defaultValue)
            for a in font["fvar"].axes
            if a.axisTag != "wght"
        }
        if pins:
            font = instancer.instantiateVariableFont(font, pins)
            # Round-trip: after instancing, the lazily loaded gvar table loses
            # .notdef and the subsetter fails on it.
            buf = io.BytesIO()
            font.save(buf)
            buf.seek(0)
            font = TTFont(buf)
    opts = subset.Options()
    opts.layout_features = ["*"]  # Arabic joining, Hebrew marks, kerning
    opts.name_IDs = ["*"]  # keeps copyright + OFL licence fields
    opts.name_languages = ["*"]
    opts.name_legacy = True
    opts.notdef_outline = True
    opts.hinting = False
    opts.glyph_names = False
    opts.legacy_kern = True
    subsetter = subset.Subsetter(opts)
    subsetter.populate(unicodes=[u for a, b in UNICODE_RANGES for u in range(a, b + 1)])
    subsetter.subset(font)
    return font


def main() -> None:
    download = os.path.abspath(sys.argv[1])
    os.makedirs(download, exist_ok=True)
    os.makedirs(RES_FONT, exist_ok=True)
    os.makedirs(LICENSES, exist_ok=True)
    os.makedirs(os.path.dirname(APK_LICENSE), exist_ok=True)

    licence_texts = {}
    for res_name, (directory, file_name) in FONTS.items():
        src = os.path.join(download, f"{directory}--{file_name}")
        fetch(f"{BASE}/{directory}/{urllib.request.quote(file_name)}", src)
        out = os.path.join(RES_FONT, f"{res_name}.ttf")
        if res_name in UNMODIFIED:
            with open(src, "rb") as f, open(out, "wb") as o:
                o.write(f.read())
        else:
            shrink(src).save(out)
        if directory not in licence_texts:
            lic = os.path.join(LICENSES, f"OFL-{directory}.txt")
            fetch(f"{BASE}/{directory}/OFL.txt", lic)
            with open(lic, encoding="utf-8") as f:
                licence_texts[directory] = f.read().strip()
        print(f"{res_name}.ttf {os.path.getsize(src) // 1024}K -> {os.path.getsize(out) // 1024}K")

    with open(APK_LICENSE, "w", encoding="utf-8", newline="\n") as f:
        f.write("Fonts bundled with NoveLauncher. Each is licensed under the SIL Open Font License 1.1.\n")
        f.write("All except Varela Round were subset and had their width/slant axes pinned (see assets/fonts/prepare_fonts.py).\n\n")
        f.write("\n\n".join(f"===== {d} =====\n{t}" for d, t in licence_texts.items()))
        f.write("\n")


if __name__ == "__main__":
    main()
