import json
import tempfile
import unittest
from pathlib import Path

from android_bridge import render_album
from photo_album import config
from photo_album.layout import create_layout, photo_size
from photo_album.models import PhotoItem

ASSETS = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"


class AndroidCoreTests(unittest.TestCase):
    def test_physical_units_and_aspect_ratios(self):
        self.assertAlmostEqual(config.cm_to_points(2.54), 72.0)
        for width, height in ((1200, 600), (600, 1200), (900, 900), (1600, 400)):
            rendered_width, rendered_height = photo_size(width, height)
            self.assertAlmostEqual(rendered_width / rendered_height, width / height)
            self.assertLessEqual(max(rendered_width, rendered_height), config.cm_to_points(5.0) + 1e-8)

    def test_maxrects_paginates_without_overlap_or_loss(self):
        items = [PhotoItem(Path(f"{index}.png"), 1200, 800, uid=str(index)) for index in range(60)]
        layout = create_layout(items)
        self.assertGreater(len(layout.pages), 1)
        placed = [p for page in layout.pages for p in page.placements]
        self.assertEqual([p.item.uid for p in placed], [item.uid for item in items])
        for page in layout.pages:
            for index, first in enumerate(page.placements):
                for second in page.placements[index + 1:]:
                    separated = (
                        first.x + first.geometry.width + config.TILE_GAP_PT <= second.x + 1e-7
                        or second.x + second.geometry.width + config.TILE_GAP_PT <= first.x + 1e-7
                        or first.y + first.geometry.height + config.TILE_GAP_PT <= second.y + 1e-7
                        or second.y + second.geometry.height + config.TILE_GAP_PT <= first.y + 1e-7
                    )
                    self.assertTrue(separated)

    def test_preview_and_pdf_share_album_layout(self):
        payload = json.dumps([
            {"path": str(ASSETS / "orizzontale.png"), "caption": "FOTO ORIZZONTALE"},
            {"path": str(ASSETS / "verticale.png"), "caption": "FOTO VERTICALE"},
            {"path": str(ASSETS / "quadrata.png"), "caption": "FOTO QUADRATA"},
        ])
        with tempfile.TemporaryDirectory() as directory:
            result = json.loads(render_album(payload, directory, True))
            self.assertTrue(result["success"], result.get("debug_error"))
            self.assertEqual(result["page_count"], len(result["preview_paths"]))
            self.assertTrue(Path(result["pdf_path"]).read_bytes().startswith(b"%PDF"))
            for preview in result["preview_paths"]:
                self.assertTrue(Path(preview).read_bytes().startswith(b"\x89PNG"))

    def test_caption_limit_is_authoritative_in_python(self):
        payload = json.dumps([{
            "path": str(ASSETS / "quadrata.png"),
            "caption": "UNA DIDASCALIA CON TROPPE PAROLE",
        }])
        with tempfile.TemporaryDirectory() as directory:
            result = json.loads(render_album(payload, directory, False))
            self.assertFalse(result["success"])
            self.assertIn("massimo 4 parole", result["user_error"])

    def test_preview_only_does_not_publish_a_pdf_path(self):
        payload = json.dumps([{
            "path": str(ASSETS / "orizzontale.png"),
            "caption": "SOLO ANTEPRIMA",
        }])
        with tempfile.TemporaryDirectory() as directory:
            result = json.loads(render_album(payload, directory, False))
            self.assertTrue(result["success"])
            self.assertIsNone(result["pdf_path"])
            self.assertEqual(1, len(result["preview_paths"]))


if __name__ == "__main__":
    unittest.main()
