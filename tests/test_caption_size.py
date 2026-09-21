import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from PIL import Image

from android_bridge import render_album
from photo_album import config
from photo_album.layout import create_layout
from photo_album.models import PhotoItem
from photo_album.typography import caption_layout, resolve_font
from reportlab.pdfbase import pdfmetrics


ASSETS = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"


class CaptionSizeTests(unittest.TestCase):
    def test_medium_is_the_historical_default_and_sizes_are_ordered(self):
        self.assertEqual("medium", config.DEFAULT_CAPTION_SIZE)
        self.assertEqual(10.0, config.CAPTION_FONT_SIZE_PT)
        sizes = [
            config.caption_font_size(config.CAPTION_SIZE_SMALL),
            config.caption_font_size(config.CAPTION_SIZE_MEDIUM),
            config.caption_font_size(config.CAPTION_SIZE_LARGE),
        ]
        self.assertEqual([9.0, 10.0, 11.0], sizes)
        self.assertLess(sizes[0], sizes[1])
        self.assertLess(sizes[1], sizes[2])

    def test_short_caption_uses_selected_size(self):
        for caption_size, expected_points in config.CAPTION_FONT_SIZES_PT.items():
            with self.subTest(caption_size=caption_size):
                lines, actual_points = caption_layout("FOTO", 200, caption_size)
                self.assertEqual(["FOTO"], lines)
                self.assertEqual(expected_points, actual_points)

    def test_wrapping_has_at_most_two_lines_and_never_overflows(self):
        font_name, _ = resolve_font()
        available_width = config.cm_to_points(5) - 2 * config.CAPTION_HORIZONTAL_PADDING_PT
        captions = ("DIDASCALIA MOLTO LUNGA OGGI", "PAROLALUNGHISSIMA" * 8)
        for caption_size in config.CAPTION_FONT_SIZES_PT:
            for caption in captions:
                with self.subTest(caption_size=caption_size, caption=caption[:20]):
                    lines, font_size = caption_layout(caption, available_width, caption_size)
                    self.assertLessEqual(len(lines), 2)
                    self.assertTrue(lines)
                    for line in lines:
                        self.assertLessEqual(
                            pdfmetrics.stringWidth(line, font_name, font_size),
                            available_width + 1e-7,
                        )

    def test_two_large_lines_fit_inside_the_unchanged_caption_band(self):
        large = config.caption_font_size(config.CAPTION_SIZE_LARGE)
        block_height = 2 * large + config.CAPTION_LINE_GAP_PT
        self.assertLessEqual(block_height, config.TILE_CAPTION_AREA_PT)

    def test_bridge_passes_one_selection_to_preview_and_pdf(self):
        payload = json.dumps([{
            "path": str(ASSETS / "quadrata.png"),
            "caption": "FOTO TEST",
        }])
        for selected_size in config.CAPTION_FONT_SIZES_PT:
            observed = []

            def capture_preview(
                layout, page_index, dpi=config.PREVIEW_DPI,
                caption_size=config.DEFAULT_CAPTION_SIZE,
            ):
                observed.append(("preview", caption_size))
                return Image.new("RGB", (10, 10), "white")

            def capture_pdf(
                layout, destination,
                caption_size=config.DEFAULT_CAPTION_SIZE,
            ):
                observed.append(("pdf", caption_size))
                Path(destination).write_bytes(b"%PDF-test")

            with tempfile.TemporaryDirectory() as directory:
                with patch("android_bridge.render_preview_page", capture_preview), patch(
                    "android_bridge.create_pdf", capture_pdf,
                ):
                    result = json.loads(render_album(
                        payload,
                        directory,
                        True,
                        caption_size=selected_size,
                    ))

            self.assertTrue(result["success"], result.get("debug_error"))
            self.assertEqual(
                [("preview", selected_size), ("pdf", selected_size)],
                observed,
            )

    def test_preview_and_pdf_renderers_use_the_selected_typography(self):
        item = PhotoItem(ASSETS / "quadrata.png", 256, 256, caption="FOTO TEST")
        layout = create_layout([item])

        from photo_album.rendering import create_pdf, render_preview_page

        with patch(
            "photo_album.rendering.caption_layout",
            wraps=caption_layout,
        ) as preview_layout:
            render_preview_page(layout, 0, caption_size=config.CAPTION_SIZE_SMALL)
        self.assertTrue(preview_layout.call_args_list)
        self.assertTrue(all(
            call.args[2] == config.CAPTION_SIZE_SMALL
            for call in preview_layout.call_args_list
        ))

        with tempfile.TemporaryDirectory() as directory:
            with patch(
                "photo_album.rendering.caption_layout",
                wraps=caption_layout,
            ) as pdf_layout:
                create_pdf(
                    layout,
                    Path(directory) / "album.pdf",
                    caption_size=config.CAPTION_SIZE_LARGE,
                )
        self.assertTrue(pdf_layout.call_args_list)
        self.assertTrue(all(
            call.args[2] == config.CAPTION_SIZE_LARGE
            for call in pdf_layout.call_args_list
        ))

    def test_pdf_and_preview_keep_caption_horizontally_centered(self):
        item = PhotoItem(ASSETS / "quadrata.png", 256, 256, caption="FOTO TEST")
        layout = create_layout([item])
        placement = layout.pages[0].placements[0]
        expected_center = placement.x + placement.geometry.width / 2
        pdf_centers = []
        preview_text_calls = []

        class RecordingCanvas:
            def __init__(self, *args, **kwargs):
                pass

            def drawCentredString(self, x, y, text):
                pdf_centers.append(x)

            def __getattr__(self, name):
                return lambda *args, **kwargs: None

        class RecordingDraw:
            def rectangle(self, *args, **kwargs):
                pass

            def text(self, position, text, **kwargs):
                preview_text_calls.append((position, kwargs.get("anchor")))

        from photo_album.rendering import create_pdf, render_preview_page

        with tempfile.TemporaryDirectory() as directory:
            with patch("photo_album.rendering.Canvas", RecordingCanvas):
                create_pdf(layout, Path(directory) / "album.pdf")
            with patch("photo_album.rendering.ImageDraw.Draw", return_value=RecordingDraw()):
                render_preview_page(layout, 0)

        self.assertTrue(pdf_centers)
        self.assertTrue(all(abs(x - expected_center) < 1e-7 for x in pdf_centers))
        self.assertTrue(preview_text_calls)
        expected_preview_center = expected_center * config.PREVIEW_DPI / 72.0
        for (x, _), anchor in preview_text_calls:
            self.assertAlmostEqual(expected_preview_center, x)
            self.assertEqual("ma", anchor)

    def test_large_caption_keeps_multi_page_layout(self):
        items = [
            PhotoItem(
                ASSETS / "orizzontale.png",
                320,
                180,
                caption=f"FOTO {index}",
                uid=str(index),
            )
            for index in range(60)
        ]
        layout = create_layout(items)
        self.assertGreater(len(layout.pages), 1)
        placed = [placement.item.uid for page in layout.pages for placement in page.placements]
        self.assertEqual([item.uid for item in items], placed)


if __name__ == "__main__":
    unittest.main()
