"""Genera una volta i tre asset PNG identificabili dello spike."""

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"
ROOT.mkdir(parents=True, exist_ok=True)

specs = (
    ("orizzontale.png", (1200, 600), "ORIZZONTALE", "#2c6eaf"),
    ("verticale.png", (600, 1200), "VERTICALE", "#d07035"),
    ("quadrata.png", (900, 900), "QUADRATA", "#4d9355"),
)
for filename, size, label, color in specs:
    image = Image.new("RGB", size, color)
    draw = ImageDraw.Draw(image)
    draw.rectangle((20, 20, size[0] - 21, size[1] - 21), outline="white", width=12)
    draw.line((0, 0, size[0], size[1]), fill="white", width=8)
    draw.line((size[0], 0, 0, size[1]), fill="white", width=8)
    draw.text((size[0] // 2, size[1] // 2), label, fill="white", anchor="mm", stroke_width=2, stroke_fill="black")
    image.save(ROOT / filename, optimize=True)

