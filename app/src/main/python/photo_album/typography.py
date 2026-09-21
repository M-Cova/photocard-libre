"""Tipografia portabile: font TTF incluso, senza percorsi del sistema host."""

from functools import lru_cache
from pathlib import Path

from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

from . import config

FONT_NAME = "DejaVuSans"
FONT_PATH = Path(__file__).parent / "assets" / "DejaVuSans.ttf"


@lru_cache(maxsize=1)
def resolve_font() -> tuple[str, str]:
    if not FONT_PATH.is_file():
        raise FileNotFoundError(f"Font incluso non trovato: {FONT_PATH}")
    pdfmetrics.registerFont(TTFont(FONT_NAME, str(FONT_PATH)))
    return FONT_NAME, str(FONT_PATH)


def _width(text: str, font_name: str, size: float) -> float:
    return pdfmetrics.stringWidth(text, font_name, size)


def caption_layout(
    text: str,
    available_width: float,
    caption_size: str = config.DEFAULT_CAPTION_SIZE,
) -> tuple[list[str], float]:
    preferred_size = config.caption_font_size(caption_size)
    if not text:
        return [], preferred_size
    font_name, _ = resolve_font()
    words = text.split()
    for size_tenths in range(
        int(preferred_size * 10),
        int(config.CAPTION_MIN_FONT_SIZE_PT * 10) - 1,
        -1,
    ):
        size = size_tenths / 10
        if _width(text, font_name, size) <= available_width:
            return [text], size
        candidates: list[tuple[float, list[str]]] = []
        for split in range(1, len(words)):
            lines = [" ".join(words[:split]), " ".join(words[split:])]
            widest = max(_width(line, font_name, size) for line in lines)
            if widest <= available_width:
                candidates.append((widest, lines))
        if candidates:
            return min(candidates, key=lambda candidate: (candidate[0], candidate[1]))[1], size
    size = min(
        config.CAPTION_MIN_FONT_SIZE_PT,
        available_width / _width(text, font_name, 1.0),
    )
    return [text], size
