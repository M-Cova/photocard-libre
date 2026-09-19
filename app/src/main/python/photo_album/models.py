"""Modelli dati indipendenti dalla GUI."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from uuid import uuid4

from . import config


class CaptionError(ValueError):
    pass


@dataclass(frozen=True)
class NormalizedCrop:
    left: float
    top: float
    width: float
    height: float

    def __post_init__(self) -> None:
        values = (self.left, self.top, self.width, self.height)
        if not all(isinstance(value, (int, float)) for value in values):
            raise ValueError("Parametri crop non validi.")
        if self.left < 0 or self.top < 0 or self.width <= 0 or self.height <= 0:
            raise ValueError("Parametri crop non validi.")
        if self.left + self.width > 1.0 + 1e-9 or self.top + self.height > 1.0 + 1e-9:
            raise ValueError("Il crop deve rimanere entro l'immagine.")

    @classmethod
    def from_dict(cls, values: dict | None) -> "NormalizedCrop | None":
        if values is None:
            return None
        return cls(
            left=float(values["left"]),
            top=float(values["top"]),
            width=float(values["width"]),
            height=float(values["height"]),
        )


def normalize_caption(text: str) -> str:
    normalized = " ".join(text.strip().split()).upper()
    if len(normalized.split()) > config.MAX_CAPTION_WORDS:
        raise CaptionError("La didascalia può contenere al massimo 4 parole.")
    return normalized


@dataclass
class PhotoItem:
    path: Path
    pixel_width: int
    pixel_height: int
    caption: str = ""
    crop: NormalizedCrop | None = None
    uid: str = field(default_factory=lambda: uuid4().hex)

    def __post_init__(self) -> None:
        if self.pixel_width <= 0 or self.pixel_height <= 0:
            raise ValueError("Le dimensioni dell'immagine devono essere positive.")
        self.path = Path(self.path)
        self.caption = normalize_caption(self.caption)

    @property
    def aspect_ratio(self) -> float:
        return self.pixel_width / self.pixel_height

    def set_caption(self, text: str) -> None:
        self.caption = normalize_caption(text)


@dataclass(frozen=True)
class TileGeometry:
    photo_width: float
    photo_height: float
    width: float
    height: float


@dataclass(frozen=True)
class Placement:
    item: PhotoItem
    page_index: int
    x: float
    y: float
    geometry: TileGeometry


@dataclass
class LayoutPage:
    index: int
    placements: list[Placement] = field(default_factory=list)


@dataclass
class LayoutResult:
    pages: list[LayoutPage]
    page_width: float = config.PAGE_WIDTH_PT
    page_height: float = config.PAGE_HEIGHT_PT
