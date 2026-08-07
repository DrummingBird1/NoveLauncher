"""
v8: Clean logo redesign — no busy concentric rings.
Confident N glyph + 2 precision sparkles.
3 variants: full, monochrome, horizontal lockup.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, math

# Paths are relative to this script's location so it runs regardless of where
# the repo is checked out — no hardcoded sandbox path.
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(SCRIPT_DIR)
OUT_DIR = os.path.join(SCRIPT_DIR, "graphics")
RES_DIR = os.path.join(ROOT_DIR, "android", "app", "src", "main", "res")
os.makedirs(OUT_DIR, exist_ok=True)

PRIMARY = (124, 124, 255)
ACCENT = (78, 205, 196)
BG_DARK = (10, 10, 26)
BG_GRADIENT_TOP = (15, 12, 41)
BG_GRADIENT_BOT = (36, 36, 62)
TEXT_LIGHT = (255, 255, 255)
TEXT_DIM = (224, 224, 232)

def font(size, bold=True):
    path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold \
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    try: return ImageFont.truetype(path, size)
    except: return ImageFont.load_default()

def gradient(size, top, bot):
    img = Image.new("RGB", size, top)
    px = img.load(); w, h = size
    for y in range(h):
        r = y / max(1, h - 1)
        c = tuple(int(top[i] * (1 - r) + bot[i] * r) for i in range(3))
        for x in range(w): px[x, y] = c
    return img

def radial(size, center, edge):
    img = Image.new("RGB", size, edge)
    px = img.load(); w, h = size
    cx, cy = w // 2, h // 2
    max_d = math.sqrt(cx * cx + cy * cy)
    for y in range(h):
        for x in range(w):
            r = min(1.0, math.sqrt((x - cx) ** 2 + (y - cy) ** 2) / max_d)
            c = tuple(int(center[i] * (1 - r) + edge[i] * r) for i in range(3))
            px[x, y] = c
    return img

def round_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size, size), radius=radius, fill=255)
    return m

def draw_n(canvas, cx, cy, glyph_size, color, stroke_ratio=0.18):
    """Clean N glyph — no rings, balanced proportions."""
    draw = ImageDraw.Draw(canvas)
    half = glyph_size // 2
    stroke = int(glyph_size * stroke_ratio)
    left, right = cx - half, cx + half
    top, bot = cy - half, cy + half

    # Verticals with slight rounding
    draw.rounded_rectangle((left, top, left + stroke, bot), radius=stroke // 5, fill=color)
    draw.rounded_rectangle((right - stroke, top, right, bot), radius=stroke // 5, fill=color)

    # Diagonal — thicker for visual weight
    diag = int(stroke * 1.05)
    draw.polygon([
        (left + stroke - 1, top),
        (left + stroke + diag, top),
        (right - stroke + 1, bot),
        (right - stroke - diag, bot),
    ], fill=color)

def draw_sparkles(canvas, cx, cy, glyph_size):
    draw = ImageDraw.Draw(canvas)
    s1 = int(glyph_size * 0.10); s2 = int(glyph_size * 0.06)
    s1x, s1y = cx + int(glyph_size * 0.65), cy - int(glyph_size * 0.55)
    s2x, s2y = cx + int(glyph_size * 0.78), cy - int(glyph_size * 0.30)
    draw.ellipse((s1x - s1, s1y - s1, s1x + s1, s1y + s1), fill=ACCENT)
    draw.ellipse((s2x - s2, s2y - s2, s2x + s2, s2y + s2), fill=TEXT_LIGHT)

def variant_full(size=512, output=None):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = radial((size, size), (45, 45, 80), BG_DARK).convert("RGBA")
    img.paste(bg, (0, 0), round_mask(size, int(size * 0.22)))

    # Soft inner glow
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx, cy = size // 2, size // 2
    gr = int(size * 0.32)
    ImageDraw.Draw(glow).ellipse(
        (cx - gr, cy - gr, cx + gr, cy + gr),
        fill=(PRIMARY[0], PRIMARY[1], PRIMARY[2], 50)
    )
    glow = glow.filter(ImageFilter.GaussianBlur(radius=size // 16))
    img = Image.alpha_composite(img, glow)

    glyph = int(size * 0.42)
    draw_n(img, cx, cy, glyph, PRIMARY)
    draw_sparkles(img, cx, cy, glyph)

    if output: img.save(output, "PNG")
    return img

def variant_mono(size=512, output=None):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx, cy = size // 2, size // 2
    draw_n(img, cx, cy, int(size * 0.50), TEXT_LIGHT)
    if output: img.save(output, "PNG")
    return img

def variant_horizontal(w=1024, h=320, output=None):
    img = gradient((w, h), BG_GRADIENT_TOP, BG_GRADIENT_BOT).convert("RGBA")
    isz = int(h * 0.7)
    icon = variant_full(isz)
    img.paste(icon, (int(h * 0.15), (h - isz) // 2), icon)

    draw = ImageDraw.Draw(img)
    text_x = int(h * 0.15) + isz + int(h * 0.15)
    # Smaller font so wordmark fits within canvas
    draw.text((text_x, int(h * 0.30)), "NoveLauncher",
              font=font(int(h * 0.24), True), fill=TEXT_LIGHT)
    draw.text((text_x + 2, int(h * 0.30) + int(h * 0.30)),
              "AI Smart Launcher", font=font(int(h * 0.10), False), fill=ACCENT)

    if output: img.convert("RGB").save(output, "PNG")
    return img

def feature_graphic():
    w, h = 1024, 500
    img = gradient((w, h), BG_GRADIENT_TOP, BG_GRADIENT_BOT).convert("RGBA")
    ovl = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    od = ImageDraw.Draw(ovl)
    od.ellipse((-100, 200, 400, 700), fill=PRIMARY + (50,))
    od.ellipse((700, -200, 1200, 300), fill=ACCENT + (40,))
    ovl = ovl.filter(ImageFilter.GaussianBlur(radius=80))
    img = Image.alpha_composite(img, ovl)

    icon = variant_full(220)
    img.paste(icon, (60, (h - 220) // 2), icon)

    draw = ImageDraw.Draw(img)
    draw.text((320, 130), "NoveLauncher", font=font(72, True), fill=TEXT_LIGHT)
    draw.text((322, 215), "AI-Powered Smart Launcher", font=font(28, False), fill=ACCENT)

    y = 280
    for f in ["AI app prediction", "12 themes & icon shapes", "Smart folders & widgets"]:
        cx, cy = 332, y + 14
        draw.line([(cx - 8, cy), (cx - 2, cy + 6), (cx + 10, cy - 8)], fill=ACCENT, width=4)
        draw.text((352, y), f, font=font(22, False), fill=TEXT_DIM)
        y += 38

    img.convert("RGB").save(os.path.join(OUT_DIR, "feature-graphic-1024x500.png"), "PNG", quality=95)

variant_full(512, os.path.join(OUT_DIR, "playstore-icon-512.png"))
variant_full(1024, os.path.join(OUT_DIR, "playstore-icon-1024.png"))
variant_mono(512, os.path.join(OUT_DIR, "logo-monochrome-512.png"))
variant_horizontal(1024, 320, os.path.join(OUT_DIR, "logo-horizontal-1024x320.png"))

for name, sz in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    icon = variant_full(sz * 4).resize((sz, sz), Image.LANCZOS)
    d = os.path.join(RES_DIR, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    icon.save(os.path.join(d, "ic_launcher.png"), "PNG")
    icon.save(os.path.join(d, "ic_launcher_round.png"), "PNG")

feature_graphic()
print("✓ v8 clean logo system generated")
