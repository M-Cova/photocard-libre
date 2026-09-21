"""Misure fisiche e impostazioni modificabili dell'applicazione."""

from reportlab.lib.pagesizes import A4

POINTS_PER_INCH = 72.0
CM_PER_INCH = 2.54


def cm_to_points(value: float) -> float:
    return value * POINTS_PER_INCH / CM_PER_INCH


PAGE_WIDTH_PT, PAGE_HEIGHT_PT = A4
PAGE_MARGIN_PT = cm_to_points(1.0)
DEFAULT_MAX_PHOTO_SIDE_CM = 5
SUPPORTED_MAX_PHOTO_SIDE_CM = (5, 7, 10)

TILE_SIDE_PADDING_PT = cm_to_points(0.22)
TILE_TOP_PADDING_PT = cm_to_points(0.22)
TILE_CAPTION_AREA_PT = cm_to_points(0.90)
TILE_BORDER_PT = 0.7
CARD_SPACING_PT = cm_to_points(0.7)

CAPTION_SIZE_SMALL = "small"
CAPTION_SIZE_MEDIUM = "medium"
CAPTION_SIZE_LARGE = "large"
CAPTION_FONT_SIZES_PT = {
    CAPTION_SIZE_SMALL: 9.0,
    CAPTION_SIZE_MEDIUM: 10.0,
    CAPTION_SIZE_LARGE: 11.0,
}
DEFAULT_CAPTION_SIZE = CAPTION_SIZE_MEDIUM
# Alias compatibile con il valore storico: Media mantiene esattamente 10 pt.
CAPTION_FONT_SIZE_PT = CAPTION_FONT_SIZES_PT[DEFAULT_CAPTION_SIZE]
CAPTION_MIN_FONT_SIZE_PT = 6.5
CAPTION_LINE_GAP_PT = 1.5
CAPTION_HORIZONTAL_PADDING_PT = cm_to_points(0.12)
MAX_CAPTION_WORDS = 4


def caption_font_size(caption_size: str) -> float:
    try:
        return CAPTION_FONT_SIZES_PT[caption_size]
    except KeyError as error:
        raise ValueError("Dimensione didascalia PDF non supportata.") from error

SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png"}
PREVIEW_DPI = 92

# A 5 cm, 300 DPI richiedono circa 591 pixel. Il margine fino a 750 pixel
# evita perdita visibile senza incorporare nel PDF i raster originali enormi.
PDF_IMAGE_MAX_SIDE_PX = 750
PDF_IMAGE_JPEG_QUALITY = 88
