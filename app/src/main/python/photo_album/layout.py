"""Motore deterministico MaxRects con coordinate fisiche in punti PDF."""

from __future__ import annotations

from dataclasses import dataclass

from . import config
from .models import LayoutPage, LayoutResult, PhotoItem, Placement, TileGeometry

EPSILON = 1e-7


@dataclass(frozen=True)
class Rect:
    x: float
    y: float
    width: float
    height: float

    @property
    def right(self) -> float:
        return self.x + self.width

    @property
    def bottom(self) -> float:
        return self.y + self.height


def photo_size(
    pixel_width: int,
    pixel_height: int,
    max_photo_side_cm: int = config.DEFAULT_MAX_PHOTO_SIDE_CM,
) -> tuple[float, float]:
    if pixel_width <= 0 or pixel_height <= 0:
        raise ValueError("Le dimensioni della fotografia devono essere positive.")
    if max_photo_side_cm not in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
        raise ValueError("Dimensione immagini PDF non supportata.")
    scale = config.cm_to_points(max_photo_side_cm) / max(pixel_width, pixel_height)
    return pixel_width * scale, pixel_height * scale


def tile_geometry(
    item: PhotoItem,
    max_photo_side_cm: int = config.DEFAULT_MAX_PHOTO_SIDE_CM,
) -> TileGeometry:
    photo_width, photo_height = photo_size(
        item.pixel_width,
        item.pixel_height,
        max_photo_side_cm,
    )
    return TileGeometry(
        photo_width=photo_width,
        photo_height=photo_height,
        width=photo_width + 2 * config.TILE_SIDE_PADDING_PT,
        height=photo_height + config.TILE_TOP_PADDING_PT + config.TILE_CAPTION_AREA_PT,
    )


def _intersects(a: Rect, b: Rect) -> bool:
    return not (
        b.x >= a.right - EPSILON
        or b.right <= a.x + EPSILON
        or b.y >= a.bottom - EPSILON
        or b.bottom <= a.y + EPSILON
    )


def _split(free: Rect, used: Rect) -> list[Rect]:
    if not _intersects(free, used):
        return [free]
    result: list[Rect] = []
    if used.x > free.x + EPSILON:
        result.append(Rect(free.x, free.y, used.x - free.x, free.height))
    if used.right < free.right - EPSILON:
        result.append(Rect(used.right, free.y, free.right - used.right, free.height))
    if used.y > free.y + EPSILON:
        result.append(Rect(free.x, free.y, free.width, used.y - free.y))
    if used.bottom < free.bottom - EPSILON:
        result.append(Rect(free.x, used.bottom, free.width, free.bottom - used.bottom))
    return [rect for rect in result if rect.width > EPSILON and rect.height > EPSILON]


def _contains(outer: Rect, inner: Rect) -> bool:
    return (
        outer.x <= inner.x + EPSILON
        and outer.y <= inner.y + EPSILON
        and outer.right >= inner.right - EPSILON
        and outer.bottom >= inner.bottom - EPSILON
    )


def _prune(rectangles: list[Rect]) -> list[Rect]:
    unique: list[Rect] = []
    for rect in rectangles:
        if rect not in unique:
            unique.append(rect)
    return [
        rect
        for index, rect in enumerate(unique)
        if not any(
            _contains(other, rect)
            for other_index, other in enumerate(unique)
            if other_index != index
        )
    ]


class _PagePacker:
    def __init__(self) -> None:
        usable_width = config.PAGE_WIDTH_PT - 2 * config.PAGE_MARGIN_PT
        usable_height = config.PAGE_HEIGHT_PT - 2 * config.PAGE_MARGIN_PT
        self.free = [
            Rect(
                config.PAGE_MARGIN_PT,
                config.PAGE_MARGIN_PT,
                usable_width + config.TILE_GAP_PT,
                usable_height + config.TILE_GAP_PT,
            )
        ]

    def place(self, geometry: TileGeometry) -> tuple[float, float] | None:
        reserved_width = geometry.width + config.TILE_GAP_PT
        reserved_height = geometry.height + config.TILE_GAP_PT
        fitting = [
            rect
            for rect in self.free
            if reserved_width <= rect.width + EPSILON
            and reserved_height <= rect.height + EPSILON
        ]
        if not fitting:
            return None
        chosen = min(
            fitting,
            key=lambda rect: (
                rect.y + reserved_height,
                rect.x,
                rect.width * rect.height - reserved_width * reserved_height,
                min(rect.width - reserved_width, rect.height - reserved_height),
            ),
        )
        used = Rect(chosen.x, chosen.y, reserved_width, reserved_height)
        split_rectangles: list[Rect] = []
        for rect in self.free:
            split_rectangles.extend(_split(rect, used))
        self.free = _prune(split_rectangles)
        return chosen.x, chosen.y


def create_layout(
    items: list[PhotoItem],
    max_photo_side_cm: int = config.DEFAULT_MAX_PHOTO_SIDE_CM,
) -> LayoutResult:
    if not items:
        return LayoutResult(pages=[])
    pages: list[LayoutPage] = [LayoutPage(index=0)]
    packers = [_PagePacker()]
    for item in items:
        geometry = tile_geometry(item, max_photo_side_cm)
        page_index = len(pages) - 1
        position = packers[-1].place(geometry)
        if position is None:
            page_index = len(pages)
            pages.append(LayoutPage(index=page_index))
            packer = _PagePacker()
            packers.append(packer)
            position = packer.place(geometry)
            if position is None:
                raise ValueError("Una tessera è troppo grande per la pagina A4.")
        x, y = position
        pages[page_index].placements.append(
            Placement(item=item, page_index=page_index, x=x, y=y, geometry=geometry)
        )
    return LayoutResult(pages=pages)
