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
TILE_GAP_PT = cm_to_points(0.35)

CAPTION_FONT_SIZE_PT = 10.0
CAPTION_MIN_FONT_SIZE_PT = 6.5
CAPTION_LINE_GAP_PT = 1.5
CAPTION_HORIZONTAL_PADDING_PT = cm_to_points(0.12)
MAX_CAPTION_WORDS = 4

SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png"}
PREVIEW_DPI = 92

# A 5 cm, 300 DPI richiedono circa 591 pixel. Il margine fino a 750 pixel
# evita perdita visibile senza incorporare nel PDF i raster originali enormi.
PDF_IMAGE_MAX_SIDE_PX = 750
PDF_IMAGE_JPEG_QUALITY = 88
