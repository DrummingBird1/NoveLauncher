"""
v9.3: brand assets for the public GitHub repo.

Generates:
  graphics/readme-banner.png        — hero banner for README.md (and the site)
  graphics/releases/vX.Y.Z.png      — one branded card per release

Everything is drawn at SUPERSAMPLE× and downscaled with LANCZOS, so edges and
the N glyph stay crisp instead of showing the jaggies PIL's raw draw calls
produce. Colour palette matches generate_graphics.py (the store/app assets) so
the repo, the site and the Play listing all look like the same product.

Run:  uv run --with pillow python assets/generate_brand_assets.py
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os
import math

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(SCRIPT_DIR, "graphics")
REL_DIR = os.path.join(OUT_DIR, "releases")
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(REL_DIR, exist_ok=True)

# ── Brand palette (identical to generate_graphics.py) ──────────────────
PRIMARY = (124, 124, 255)      # #7C7CFF
ACCENT = (78, 205, 196)        # #4ECDC4
BG_TOP = (15, 12, 41)          # #0F0C29
BG_BOT = (26, 26, 48)          # #1A1A30
TEXT = (255, 255, 255)
TEXT_DIM = (168, 168, 196)

SUPERSAMPLE = 3


def font(size, bold=True):
    """Resolve a real font on Windows/macOS/Linux — never fall back to PIL's
    tiny bitmap default, which would make every heading look broken."""
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold
        else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def backdrop(w, h):
    """Diagonal gradient + soft indigo/teal glows."""
    img = Image.new("RGB", (w, h), BG_TOP)
    px = img.load()
    for y in range(h):
        for x in range(w):
            # diagonal ramp so the corner-to-corner sweep reads as depth
            r = (x / w * 0.35) + (y / h * 0.65)
            px[x, y] = tuple(int(BG_TOP[i] * (1 - r) + BG_BOT[i] * r) for i in range(3))

    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((-w * 0.15, -h * 0.55, w * 0.5, h * 0.75), fill=(38, 38, 92))
    gd.ellipse((w * 0.62, h * 0.35, w * 1.2, h * 1.5), fill=(16, 52, 58))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=max(w, h) // 7))
    return Image.blend(img, Image.blend(img, glow, 0.55), 0.9)


def draw_n(draw, cx, cy, size, color, stroke_ratio=0.18):
    """The NoveLauncher N — same geometry as the app icon."""
    half = size // 2
    stroke = int(size * stroke_ratio)
    left, right = cx - half, cx + half
    top, bot = cy - half, cy + half
    draw.rounded_rectangle((left, top, left + stroke, bot), radius=stroke // 5, fill=color)
    draw.rounded_rectangle((right - stroke, top, right, bot), radius=stroke // 5, fill=color)
    diag = int(stroke * 1.05)
    draw.polygon([
        (left + stroke - 1, top),
        (left + stroke + diag, top),
        (right - stroke + 1, bot),
        (right - stroke - diag, bot),
    ], fill=color)


def draw_sparkles(draw, cx, cy, size):
    def star(x, y, s, color):
        draw.polygon([(x, y - s), (x + s * 0.32, y - s * 0.32), (x + s, y),
                      (x + s * 0.32, y + s * 0.32), (x, y + s),
                      (x - s * 0.32, y + s * 0.32), (x - s, y),
                      (x - s * 0.32, y - s * 0.32)], fill=color)
    star(cx + int(size * 0.46), cy - int(size * 0.42), int(size * 0.10), ACCENT)
    star(cx + int(size * 0.60), cy - int(size * 0.14), int(size * 0.06), TEXT)


def icon_mark(box):
    """Rounded-square app mark with radial glow, N and sparkles."""
    s = box * SUPERSAMPLE
    tile = Image.new("RGB", (s, s), (18, 16, 46))
    px = tile.load()
    cx = cy = s // 2
    max_d = math.sqrt(cx ** 2 + cy ** 2)
    for y in range(s):
        for x in range(s):
            r = min(1.0, math.sqrt((x - cx) ** 2 + (y - cy) ** 2) / max_d)
            px[x, y] = (int(56 * (1 - r) + 16 * r),
                        int(50 * (1 - r) + 14 * r),
                        int(120 * (1 - r) + 40 * r))
    d = ImageDraw.Draw(tile)
    draw_n(d, cx, cy, int(s * 0.42), PRIMARY)
    draw_sparkles(d, cx, cy, int(s * 0.42))

    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=int(s * 0.24), fill=255)
    tile.putalpha(mask)
    return tile.resize((box, box), Image.LANCZOS)


def text_w(draw, s, f):
    return draw.textbbox((0, 0), s, font=f)[2]


def banner(path, w=1280, h=420):
    W, H = w * SUPERSAMPLE, h * SUPERSAMPLE
    img = backdrop(W, H).convert("RGBA")
    d = ImageDraw.Draw(img)

    mark = icon_mark(int(200 * SUPERSAMPLE))
    mx, my = int(96 * SUPERSAMPLE), (H - mark.height) // 2
    img.alpha_composite(mark, (mx, my))

    tx = mx + mark.width + int(56 * SUPERSAMPLE)
    f_title = font(int(96 * SUPERSAMPLE))
    f_tag = font(int(34 * SUPERSAMPLE), bold=False)

    d.text((tx, int(140 * SUPERSAMPLE)), "NoveLauncher", font=f_title, fill=TEXT)
    d.text((tx, int(252 * SUPERSAMPLE)),
           "A private, on-device AI launcher for Android",
           font=f_tag, fill=TEXT_DIM)

    # accent underline
    d.rounded_rectangle(
        (tx, int(316 * SUPERSAMPLE), tx + int(150 * SUPERSAMPLE), int(324 * SUPERSAMPLE)),
        radius=int(4 * SUPERSAMPLE), fill=ACCENT)

    img.convert("RGB").resize((w, h), Image.LANCZOS).save(path, "PNG")
    print("wrote", path)


def release_card(path, version, subtitle, w=1200, h=630):
    W, H = w * SUPERSAMPLE, h * SUPERSAMPLE
    img = backdrop(W, H).convert("RGBA")
    d = ImageDraw.Draw(img)

    mark = icon_mark(int(150 * SUPERSAMPLE))
    img.alpha_composite(mark, ((W - mark.width) // 2, int(96 * SUPERSAMPLE)))

    f_name = font(int(52 * SUPERSAMPLE))
    f_ver = font(int(120 * SUPERSAMPLE))
    f_sub = font(int(30 * SUPERSAMPLE), bold=False)

    name = "NoveLauncher"
    d.text(((W - text_w(d, name, f_name)) // 2, int(280 * SUPERSAMPLE)),
           name, font=f_name, fill=TEXT_DIM)

    ver = f"v{version}"
    d.text(((W - text_w(d, ver, f_ver)) // 2, int(348 * SUPERSAMPLE)),
           ver, font=f_ver, fill=TEXT)

    d.text(((W - text_w(d, subtitle, f_sub)) // 2, int(500 * SUPERSAMPLE)),
           subtitle, font=f_sub, fill=ACCENT)

    bar_w = int(120 * SUPERSAMPLE)
    d.rounded_rectangle(((W - bar_w) // 2, int(560 * SUPERSAMPLE),
                         (W + bar_w) // 2, int(568 * SUPERSAMPLE)),
                        radius=int(4 * SUPERSAMPLE), fill=PRIMARY)

    img.convert("RGB").resize((w, h), Image.LANCZOS).save(path, "PNG")
    print("wrote", path)


RELEASES = [
    ("8.0.0", "Logo redesign, splash screen, privacy policy"),
    ("9.0.0", "Full i18n, hardened crypto, wired-up features"),
    ("9.1.0", "Accessibility, encrypted backups, dynamic colour"),
    ("9.2.0", "Language picker, custom RSS, quick-settings tile"),
    ("9.3.0", "Infrastructure, testing and performance overhaul"),
]

if __name__ == "__main__":
    banner(os.path.join(OUT_DIR, "readme-banner.png"))
    for version, subtitle in RELEASES:
        release_card(os.path.join(REL_DIR, f"v{version}.png"), version, subtitle)
