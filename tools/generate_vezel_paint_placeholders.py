"""
Generate original placeholder Vezel paint WebP assets (not OEM photography).
Six consistent silhouettes at practical dashboard bounding size.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
W, H = 720, 360

PAINTS = [
    ("blue_gray", (42, 58, 82), "Blue Gray"),
    ("white", (216, 222, 233), "White"),
    ("red", (179, 58, 58), "Red"),
    ("black", (26, 31, 42), "Black"),
    ("silver", (168, 176, 188), "Silver"),
    ("blue", (46, 90, 160), "Blue"),
]


def make_silhouette(body_rgb: tuple[int, int, int], label: str) -> Image.Image:
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.ellipse((120, 280, 600, 330), fill=(0, 0, 0, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(12))
    img = Image.alpha_composite(img, shadow)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    r, g, b = body_rgb
    gd.ellipse((150, 250, 570, 320), fill=(r, g, b, 55))
    glow = glow.filter(ImageFilter.GaussianBlur(18))
    img = Image.alpha_composite(img, glow)

    draw = ImageDraw.Draw(img)
    wheel_y = 250
    for cx in (210, 500):
        draw.ellipse((cx - 38, wheel_y - 38, cx + 38, wheel_y + 38), fill=(20, 22, 28, 255))
        draw.ellipse((cx - 22, wheel_y - 22, cx + 22, wheel_y + 22), fill=(70, 78, 90, 255))
        draw.ellipse((cx - 8, wheel_y - 8, cx + 8, wheel_y + 8), fill=(140, 148, 160, 255))

    body = [
        (140, 230), (160, 195), (200, 185), (240, 145), (360, 125),
        (480, 135), (540, 155), (580, 185), (600, 200), (610, 230),
        (580, 245), (150, 245),
    ]
    draw.polygon(body, fill=body_rgb + (255,))

    glass = [
        (250, 150), (360, 132), (470, 142), (520, 165),
        (500, 185), (260, 185),
    ]
    draw.polygon(glass, fill=(30, 40, 55, 200))

    hl = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hl)
    light = tuple(min(255, c + 40) for c in body_rgb)
    hd.polygon(
        [
            (170, 220), (200, 190), (250, 150), (360, 132), (470, 142),
            (460, 148), (360, 140), (255, 156), (210, 192), (185, 218),
        ],
        fill=light + (90,),
    )
    img = Image.alpha_composite(img, hl)
    draw = ImageDraw.Draw(img)

    draw.ellipse((575, 200, 600, 220), fill=(240, 230, 180, 220))
    draw.rounded_rectangle((145, 205, 165, 225), radius=4, fill=(200, 50, 50, 220))
    draw.rounded_rectangle((560, 210, 595, 230), radius=3, fill=(20, 24, 30, 180))

    try:
        font = ImageFont.truetype("arial.ttf", 18)
        small = ImageFont.truetype("arial.ttf", 14)
    except OSError:
        font = ImageFont.load_default()
        small = font
    draw.text((24, 16), "PLACEHOLDER", fill=(255, 255, 255, 160), font=font)
    draw.text((24, 40), f"Vezel paint · {label}", fill=(200, 210, 230, 140), font=small)
    draw.text((24, H - 36), "Original silhouette — not OEM art", fill=(160, 170, 190, 120), font=small)
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for key, rgb, label in PAINTS:
        path = OUT / f"vezel_{key}.webp"
        make_silhouette(rgb, label).save(path, "WEBP", quality=90, method=6)
        print(f"{path.name} {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()
