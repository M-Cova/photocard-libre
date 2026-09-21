"""Il bordo di taglio è sempre presente nel PDF e nell'anteprima."""

import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from android_bridge import render_album
from photo_album import config
from photo_album.images import load_photo
from photo_album.layout import create_layout
from photo_album.rendering import create_pdf, render_preview_page


ASSETS = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"


class CuttingBorderTest(unittest.TestCase):
    def setUp(self):
        self.layout = create_layout([load_photo(str(ASSETS / "quadrata.png"), None)], 5)
        self.placement = self.layout.pages[0].placements[0]

    def test_preview_draws_border_at_card_geometry(self):
        preview = render_preview_page(self.layout, 0)
        scale = config.PREVIEW_DPI / 72.0
        x = round(self.placement.x * scale)
        y = round(self.placement.y * scale)
        width = round(self.placement.geometry.width * scale)
        height = round(self.placement.geometry.height * scale)
        for point in ((x, y), (x + width, y), (x, y + height),
                      (x + width, y + height)):
            self.assertEqual((0, 0, 0), preview.getpixel(point))

    def test_pdf_draws_border_at_card_geometry(self):
        from photo_album import rendering

        real_canvas = rendering.Canvas
        observed = []

        def capture_canvas(*args, **kwargs):
            canvas = real_canvas(*args, **kwargs)
            real_rect = canvas.rect

            def capture_rect(*rect_args, **rect_kwargs):
                observed.append((rect_args, rect_kwargs))
                return real_rect(*rect_args, **rect_kwargs)

            canvas.rect = capture_rect
            return canvas

        with tempfile.TemporaryDirectory() as directory:
            pdf_path = Path(directory) / "album.pdf"
            with patch.object(rendering, "Canvas", side_effect=capture_canvas):
                create_pdf(self.layout, pdf_path)
            self.assertTrue(pdf_path.read_bytes().startswith(b"%PDF"))

        placement = self.placement
        expected = (
            placement.x,
            self.layout.page_height - placement.y - placement.geometry.height,
            placement.geometry.width,
            placement.geometry.height,
        )
        self.assertEqual([(expected, {"stroke": 1, "fill": 0})], observed)

    def test_bridge_keeps_border_in_preview_and_pdf(self):
        payload = json.dumps([{"path": str(ASSETS / "quadrata.png"), "caption": ""}])
        with tempfile.TemporaryDirectory() as directory:
            result = json.loads(render_album(payload, directory, True))
            self.assertTrue(result["success"], result.get("debug_error"))
            self.assertTrue(Path(result["pdf_path"]).read_bytes().startswith(b"%PDF"))
            from PIL import Image
            with Image.open(result["preview_paths"][0]) as preview:
                scale = config.PREVIEW_DPI / 72.0
                point = (round(self.placement.x * scale), round(self.placement.y * scale))
                self.assertEqual((0, 0, 0), preview.getpixel(point))


if __name__ == "__main__":
    unittest.main()
