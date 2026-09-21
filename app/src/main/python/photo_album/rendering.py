"""Rendering condiviso dello stesso layout verso PDF e anteprima bitmap."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib.colors import black
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen.canvas import Canvas

from . import config
from .images import PdfImageCache, open_for_item
from .models import LayoutResult, Placement
from .typography import caption_layout, resolve_font


def _caption(placement: Placement, caption_size: str) -> tuple[list[str], float]:
    width = placement.geometry.width - 2 * config.CAPTION_HORIZONTAL_PADDING_PT
    return caption_layout(placement.item.caption, width, caption_size)


def create_pdf(
    layout: LayoutResult,
    destination: Path | str,
    caption_size: str = config.DEFAULT_CAPTION_SIZE,
) -> None:
    if not layout.pages:
        raise ValueError("Aggiungi almeno una fotografia prima di creare il PDF.")
    destination = Path(destination)
    canvas = Canvas(str(destination), pagesize=(layout.page_width, layout.page_height))
    font_name, _ = resolve_font()
    with PdfImageCache(destination.parent) as image_cache:
        for page in layout.pages:
            for placement in page.placements:
                geometry = placement.geometry
                tile_bottom = layout.page_height - placement.y - geometry.height
                photo_x = placement.x + config.TILE_SIDE_PADDING_PT
                photo_top = placement.y + config.TILE_TOP_PADDING_PT
                photo_bottom = layout.page_height - photo_top - geometry.photo_height

                image_path = image_cache.get(placement.item)
                canvas.drawImage(
                    ImageReader(str(image_path)), photo_x, photo_bottom,
                    width=geometry.photo_width, height=geometry.photo_height,
                    preserveAspectRatio=True, mask="auto",
                )
                canvas.setStrokeColor(black)
                canvas.setLineWidth(config.TILE_BORDER_PT)
                canvas.rect(
                    placement.x, tile_bottom, geometry.width, geometry.height,
                    stroke=1, fill=0,
                )

                lines, font_size = _caption(placement, caption_size)
                if lines:
                    canvas.setFillColor(black)
                    canvas.setFont(font_name, font_size)
                    line_height = font_size + config.CAPTION_LINE_GAP_PT
                    block_height = len(lines) * line_height - config.CAPTION_LINE_GAP_PT
                    first_baseline = (
                        tile_bottom
                        + (config.TILE_CAPTION_AREA_PT + block_height) / 2
                        - font_size
                    )
                    for index, line in enumerate(lines):
                        canvas.drawCentredString(
                            placement.x + geometry.width / 2,
                            first_baseline - index * line_height,
                            line,
                        )
            canvas.showPage()
    canvas.save()


def render_preview_page(
    layout: LayoutResult,
    page_index: int,
    dpi: int = config.PREVIEW_DPI,
    caption_size: str = config.DEFAULT_CAPTION_SIZE,
) -> Image.Image:
    if page_index < 0 or page_index >= len(layout.pages):
        raise IndexError("Pagina di anteprima inesistente.")
    scale = dpi / 72.0
    page_image = Image.new(
        "RGB",
        (round(layout.page_width * scale), round(layout.page_height * scale)),
        "white",
    )
    draw = ImageDraw.Draw(page_image)
    _, font_path = resolve_font()
    for placement in layout.pages[page_index].placements:
        geometry = placement.geometry
        x = round(placement.x * scale)
        y = round(placement.y * scale)
        tile_width = round(geometry.width * scale)
        tile_height = round(geometry.height * scale)
        px = round((placement.x + config.TILE_SIDE_PADDING_PT) * scale)
        py = round((placement.y + config.TILE_TOP_PADDING_PT) * scale)
        pw = max(1, round(geometry.photo_width * scale))
        ph = max(1, round(geometry.photo_height * scale))
        image = open_for_item(placement.item).resize((pw, ph), Image.Resampling.LANCZOS)
        page_image.paste(image, (px, py))
        border = max(1, round(config.TILE_BORDER_PT * scale))
        draw.rectangle((x, y, x + tile_width, y + tile_height), outline="black", width=border)

        lines, font_size = _caption(placement, caption_size)
        if lines:
            pixel_font_size = max(1, round(font_size * scale))
            font = ImageFont.truetype(font_path, pixel_font_size)
            line_height = (font_size + config.CAPTION_LINE_GAP_PT) * scale
            band_top = (placement.y + geometry.height - config.TILE_CAPTION_AREA_PT) * scale
            block_height = len(lines) * line_height - config.CAPTION_LINE_GAP_PT * scale
            baseline_y = band_top + (config.TILE_CAPTION_AREA_PT * scale - block_height) / 2
            for index, line in enumerate(lines):
                draw.text(
                    ((placement.x + geometry.width / 2) * scale, baseline_y + index * line_height),
                    line,
                    fill="black",
                    font=font,
                    anchor="ma",
                )
    return page_image
