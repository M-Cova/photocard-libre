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


ASSETS = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"
EPSILON = 1e-7


def _items(count, dimensions=((1200, 800), (800, 1200), (900, 900))):
    return [
        PhotoItem(
            ASSETS / ("orizzontale.png", "verticale.png", "quadrata.png")[index % 3],
            *dimensions[index % len(dimensions)],
            caption=f"FOTO {index}",
            uid=str(index),
        )
        for index in range(count)
    ]


def _axis_gap(first, second):
    horizontal = max(
        second.x - (first.x + first.geometry.width),
        first.x - (second.x + second.geometry.width),
    )
    vertical = max(
        second.y - (first.y + first.geometry.height),
        first.y - (second.y + second.geometry.height),
    )
    return horizontal, vertical


class CardSpacingTests(unittest.TestCase):
    def assert_page_spacing(self, page):
        horizontal_gaps = []
        vertical_gaps = []
        for index, first in enumerate(page.placements):
            for second in page.placements[index + 1:]:
                horizontal, vertical = _axis_gap(first, second)
                separated_horizontally = horizontal >= config.CARD_SPACING_PT - EPSILON
                separated_vertically = vertical >= config.CARD_SPACING_PT - EPSILON
                self.assertTrue(
                    separated_horizontally or separated_vertically,
                    f"Card {first.item.uid} e {second.item.uid} troppo vicine",
                )
                self.assertTrue(horizontal >= -EPSILON or vertical >= -EPSILON)
                if separated_horizontally:
                    horizontal_gaps.append(horizontal)
                if separated_vertically:
                    vertical_gaps.append(vertical)
        return horizontal_gaps, vertical_gaps

    def test_spacing_is_a_physical_0_7_cm_value(self):
        self.assertAlmostEqual(config.cm_to_points(0.7), config.CARD_SPACING_PT)
        self.assertAlmostEqual(19.84251968503937, config.CARD_SPACING_PT)

    def test_horizontal_and_vertical_spacing_are_at_least_0_7_cm(self):
        layout = create_layout(_items(30, dimensions=((900, 900),)))
        horizontal_gaps = []
        vertical_gaps = []
        for page in layout.pages:
            page_horizontal, page_vertical = self.assert_page_spacing(page)
            horizontal_gaps.extend(page_horizontal)
            vertical_gaps.extend(page_vertical)
        self.assertTrue(horizontal_gaps)
        self.assertTrue(vertical_gaps)
        self.assertGreaterEqual(min(horizontal_gaps), config.CARD_SPACING_PT - EPSILON)
        self.assertGreaterEqual(min(vertical_gaps), config.CARD_SPACING_PT - EPSILON)

    def test_portrait_landscape_square_and_all_sizes_keep_order_without_loss(self):
        items = _items(75)
        expected_uids = [item.uid for item in items]
        for max_side_cm in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
            with self.subTest(max_side_cm=max_side_cm):
                first = create_layout(items, max_side_cm)
                second = create_layout(items, max_side_cm)
                self.assertGreater(len(first.pages), 1)
                placed = [placement for page in first.pages for placement in page.placements]
                self.assertEqual(expected_uids, [placement.item.uid for placement in placed])
                self.assertEqual(len(expected_uids), len(set(placement.item.uid for placement in placed)))
                self.assertEqual(
                    [
                        (placement.item.uid, placement.page_index, placement.x, placement.y)
                        for page in first.pages for placement in page.placements
                    ],
                    [
                        (placement.item.uid, placement.page_index, placement.x, placement.y)
                        for page in second.pages for placement in page.placements
                    ],
                )
                for page in first.pages:
                    self.assert_page_spacing(page)

    def test_all_sizes_captions_and_border_options_keep_card_geometry(self):
        payload = json.dumps([{
            "path": str(ASSETS / "orizzontale.png"),
            "caption": "FOTO TEST",
        }])
        for max_side_cm in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
            for caption_size in config.CAPTION_FONT_SIZES_PT:
                for cutting_border in (False, True):
                    with self.subTest(
                        max_side_cm=max_side_cm,
                        caption_size=caption_size,
                        cutting_border=cutting_border,
                    ):
                        observed = []

                        def capture_preview(layout, page_index, **kwargs):
                            observed.append((layout, kwargs))
                            return Image.new("RGB", (10, 10), "white")

                        def capture_pdf(layout, destination, **kwargs):
                            observed.append((layout, kwargs))
                            Path(destination).write_bytes(b"%PDF-test")

                        with tempfile.TemporaryDirectory() as directory:
                            with patch(
                                "android_bridge.render_preview_page", capture_preview,
                            ), patch("android_bridge.create_pdf", capture_pdf):
                                result = json.loads(render_album(
                                    payload,
                                    directory,
                                    True,
                                    max_side_cm,
                                    cutting_border,
                                    caption_size,
                                ))

                        self.assertTrue(result["success"], result.get("debug_error"))
                        self.assertEqual(2, len(observed))
                        self.assertIs(observed[0][0], observed[1][0])
                        for layout, kwargs in observed:
                            geometry = layout.pages[0].placements[0].geometry
                            self.assertAlmostEqual(
                                config.cm_to_points(max_side_cm),
                                max(geometry.photo_width, geometry.photo_height),
                            )
                            self.assertEqual(cutting_border, kwargs["cutting_border"])
                            self.assertEqual(caption_size, kwargs["caption_size"])

    def test_preview_and_pdf_receive_the_same_spaced_layout(self):
        payload = json.dumps([
            {"path": str(ASSETS / "orizzontale.png"), "caption": "ORIZZONTALE"},
            {"path": str(ASSETS / "verticale.png"), "caption": "VERTICALE"},
            {"path": str(ASSETS / "quadrata.png"), "caption": "QUADRATA"},
        ] * 12)
        observed_layouts = []

        def capture_preview(layout, page_index, **kwargs):
            observed_layouts.append(("preview", page_index, layout))
            return Image.new("RGB", (10, 10), "white")

        def capture_pdf(layout, destination, **kwargs):
            observed_layouts.append(("pdf", None, layout))
            Path(destination).write_bytes(b"%PDF-test")

        with tempfile.TemporaryDirectory() as directory:
            with patch("android_bridge.render_preview_page", capture_preview), patch(
                "android_bridge.create_pdf", capture_pdf,
            ):
                result = json.loads(render_album(payload, directory, True, 10))

        self.assertTrue(result["success"], result.get("debug_error"))
        pdf_layout = observed_layouts[-1][2]
        self.assertGreater(len(pdf_layout.pages), 1)
        self.assertTrue(all(layout is pdf_layout for _, _, layout in observed_layouts))
        for page in pdf_layout.pages:
            self.assert_page_spacing(page)


if __name__ == "__main__":
    unittest.main()
