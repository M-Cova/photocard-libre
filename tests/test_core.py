import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from PIL import Image

from android_bridge import render_album
from photo_album import config
from photo_album.layout import create_layout, photo_size
from photo_album.models import PhotoItem

ASSETS = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"


class AndroidCoreTests(unittest.TestCase):
    def test_physical_units_and_aspect_ratios(self):
        self.assertAlmostEqual(config.cm_to_points(2.54), 72.0)
        for max_side_cm in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
            for width, height in ((1200, 600), (600, 1200), (900, 900), (1600, 400)):
                rendered_width, rendered_height = photo_size(width, height, max_side_cm)
                self.assertAlmostEqual(rendered_width / rendered_height, width / height)
                self.assertAlmostEqual(
                    max(rendered_width, rendered_height),
                    config.cm_to_points(max_side_cm),
                )

    def test_default_maximum_photo_side_is_five_cm(self):
        width, height = photo_size(1200, 800)
        self.assertAlmostEqual(config.cm_to_points(5), max(width, height))

    def test_three_centimeters_is_a_supported_physical_preset(self):
        self.assertEqual((3, 5, 7, 10), config.SUPPORTED_MAX_PHOTO_SIDE_CM)
        width, height = photo_size(1200, 800, 3)
        self.assertAlmostEqual(config.cm_to_points(3), max(width, height))

    def test_only_supported_pdf_image_sizes_are_accepted(self):
        with self.assertRaisesRegex(ValueError, "non supportata"):
            photo_size(1200, 800, 6)

    def test_maxrects_paginates_without_overlap_or_loss(self):
        items = [PhotoItem(Path(f"{index}.png"), 1200, 800, uid=str(index)) for index in range(60)]
        for max_side_cm in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
            with self.subTest(max_side_cm=max_side_cm):
                layout = create_layout(items, max_side_cm)
                self.assertGreater(len(layout.pages), 1)
                placed = [p for page in layout.pages for p in page.placements]
                self.assertEqual([p.item.uid for p in placed], [item.uid for item in items])
                for page in layout.pages:
                    for index, first in enumerate(page.placements):
                        for second in page.placements[index + 1:]:
                            separated = (
                                first.x + first.geometry.width + config.CARD_SPACING_PT <= second.x + 1e-7
                                or second.x + second.geometry.width + config.CARD_SPACING_PT <= first.x + 1e-7
                                or first.y + first.geometry.height + config.CARD_SPACING_PT <= second.y + 1e-7
                                or second.y + second.geometry.height + config.CARD_SPACING_PT <= first.y + 1e-7
                            )
                            self.assertTrue(separated)

    def test_selected_size_reaches_both_preview_and_pdf(self):
        payload = json.dumps([{
            "path": str(ASSETS / "orizzontale.png"),
            "caption": "DIMENSIONE REALE",
        }])
        for max_side_cm in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
            observed = []

            def capture_preview(layout, page_index, caption_size="medium"):
                observed.append(("preview", max(
                    layout.pages[page_index].placements[0].geometry.photo_width,
                    layout.pages[page_index].placements[0].geometry.photo_height,
                )))
                return Image.new("RGB", (10, 10), "white")

            def capture_pdf(layout, destination, caption_size="medium"):
                observed.append(("pdf", max(
                    layout.pages[0].placements[0].geometry.photo_width,
                    layout.pages[0].placements[0].geometry.photo_height,
                )))
                Path(destination).write_bytes(b"%PDF-test")

            with tempfile.TemporaryDirectory() as directory:
                with patch("android_bridge.render_preview_page", capture_preview), patch(
                    "android_bridge.create_pdf", capture_pdf
                ):
                    result = json.loads(render_album(payload, directory, True, max_side_cm))

            self.assertTrue(result["success"], result.get("debug_error"))
            self.assertEqual(["preview", "pdf"], [target for target, _ in observed])
            for _, maximum_side in observed:
                self.assertAlmostEqual(config.cm_to_points(max_side_cm), maximum_side)

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

    def test_failure_response_does_not_expose_exception_details(self):
        private_details = "content://private/42 /data/user/0/photo.jpg DIDASCALIA RISERVATA"
        payload = json.dumps([{
            "path": str(ASSETS / "orizzontale.png"),
            "caption": "TEST SICUREZZA",
        }])

        with tempfile.TemporaryDirectory() as directory:
            with patch(
                "android_bridge.create_layout",
                side_effect=ValueError(private_details),
            ):
                result = json.loads(render_album(payload, directory, False))

        self.assertFalse(result["success"])
        self.assertEqual("Dati di input non validi.", result["user_error"])
        self.assertEqual("ValueError", result["debug_error"])
        self.assertNotIn(private_details, json.dumps(result))

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

    def test_failed_render_removes_partial_outputs_and_preserves_published_files(self):
        payload = json.dumps([{
            "path": str(ASSETS / "orizzontale.png"),
            "caption": "ERRORE CONTROLLATO",
        }])
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            published_preview = output / "anteprima-1.png"
            published_pdf = output / "foto-album.pdf"
            published_preview.write_bytes(b"old-preview")
            published_pdf.write_bytes(b"%PDF-old")

            def fail_after_partial_pdf(layout, destination, caption_size="medium"):
                Path(destination).write_bytes(b"partial")
                raise OSError("simulated write failure")

            with patch("android_bridge.create_pdf", fail_after_partial_pdf):
                result = json.loads(render_album(payload, directory, True))

            self.assertFalse(result["success"])
            self.assertEqual(b"old-preview", published_preview.read_bytes())
            self.assertEqual(b"%PDF-old", published_pdf.read_bytes())
            self.assertFalse((output / ".render-in-progress").exists())

    def test_successful_render_removes_obsolete_preview_without_accumulating(self):
        payload = json.dumps([{
            "path": str(ASSETS / "quadrata.png"),
            "caption": "NUOVA ANTEPRIMA",
        }])
        with tempfile.TemporaryDirectory() as directory:
            obsolete = Path(directory) / "anteprima-99.png"
            obsolete.write_bytes(b"obsolete")

            result = json.loads(render_album(payload, directory, False))

            self.assertTrue(result["success"], result.get("debug_error"))
            self.assertFalse(obsolete.exists())
            self.assertEqual(1, len(list(Path(directory).glob("anteprima-*.png"))))


if __name__ == "__main__":
    unittest.main()
