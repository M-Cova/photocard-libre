import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from PIL import Image
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen.canvas import Canvas

from photo_album import config
from photo_album.images import PdfImageCache, open_normalized, optimize_for_pdf
from photo_album.layout import create_layout
from photo_album.models import PhotoItem
from photo_album.rendering import create_pdf


class PdfImageOptimizationTests(unittest.TestCase):
    def _save_rgb(self, path: Path, size: tuple[int, int], color="navy") -> None:
        Image.new("RGB", size, color).save(path)

    def test_limits_pixels_and_preserves_aspect_ratio(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "large.png"
            output = Path(directory) / "optimized.jpg"
            self._save_rgb(source, (1600, 900))

            optimize_for_pdf(source, output)

            with Image.open(output) as optimized:
                self.assertEqual(config.PDF_IMAGE_MAX_SIDE_PX, max(optimized.size))
                self.assertAlmostEqual(1600 / 900, optimized.width / optimized.height, delta=0.005)

    def test_does_not_enlarge_small_images_or_modify_source(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "small.png"
            output = Path(directory) / "optimized.jpg"
            self._save_rgb(source, (320, 200))
            original_bytes = source.read_bytes()

            optimize_for_pdf(source, output)

            with Image.open(output) as optimized:
                self.assertEqual((320, 200), optimized.size)
            self.assertEqual(original_bytes, source.read_bytes())

    def test_applies_exif_orientation_before_resize(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "rotated.jpg"
            output = Path(directory) / "optimized.jpg"
            image = Image.new("RGB", (120, 60), "red")
            exif = image.getexif()
            exif[274] = 6
            image.save(source, exif=exif)

            optimize_for_pdf(source, output)

            with Image.open(output) as optimized:
                self.assertEqual((60, 120), optimized.size)

    def test_composites_png_alpha_on_white(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "transparent.png"
            output = Path(directory) / "optimized.jpg"
            image = Image.new("RGBA", (100, 100), (255, 0, 0, 0))
            for x in range(25, 75):
                for y in range(25, 75):
                    image.putpixel((x, y), (255, 0, 0, 255))
            image.save(source)

            optimize_for_pdf(source, output)

            with Image.open(output) as optimized:
                self.assertEqual("RGB", optimized.mode)
                red, green, blue = optimized.getpixel((0, 0))
                self.assertGreater(red, 245)
                self.assertGreater(green, 245)
                self.assertGreater(blue, 245)

    def test_reuses_one_optimized_file_per_source(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "photo.png"
            self._save_rgb(source, (1200, 800))
            with PdfImageCache(directory) as cache:
                first = cache.get(source)
                second = cache.get(source)
                self.assertEqual(first, second)
                self.assertEqual(1, len(list(first.parent.glob("*.jpg"))))

    def test_pdf_draw_size_remains_the_layout_physical_size(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "photo.png"
            self._save_rgb(source, (4000, 2000))
            item = PhotoItem(source, 4000, 2000)
            layout = create_layout([item])
            expected = layout.pages[0].placements[0].geometry
            draw_calls = []

            class RecordingCanvas:
                def __init__(self, *args, **kwargs):
                    pass

                def drawImage(self, image, x, y, width, height, **kwargs):
                    draw_calls.append((width, height))

                def __getattr__(self, name):
                    return lambda *args, **kwargs: None

            with patch("photo_album.rendering.Canvas", RecordingCanvas):
                create_pdf(layout, Path(directory) / "album.pdf")

            self.assertEqual([(expected.photo_width, expected.photo_height)], draw_calls)
            self.assertAlmostEqual(config.cm_to_points(5), max(draw_calls[0]))

    def test_pdf_draw_size_uses_each_selected_physical_limit(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "photo.png"
            self._save_rgb(source, (4000, 2000))
            item = PhotoItem(source, 4000, 2000)

            for max_side_cm in config.SUPPORTED_MAX_PHOTO_SIDE_CM:
                draw_calls = []
                layout = create_layout([item], max_side_cm)

                class RecordingCanvas:
                    def __init__(self, *args, **kwargs):
                        pass

                    def drawImage(self, image, x, y, width, height, **kwargs):
                        draw_calls.append((width, height))

                    def __getattr__(self, name):
                        return lambda *args, **kwargs: None

                with patch("photo_album.rendering.Canvas", RecordingCanvas):
                    create_pdf(layout, Path(directory) / f"album-{max_side_cm}.pdf")

                self.assertAlmostEqual(config.cm_to_points(max_side_cm), max(draw_calls[0]))

    def test_high_resolution_pdf_is_smaller_than_legacy_embedding(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            items = []
            for index in range(3):
                source = root / f"high-resolution-{index}.png"
                noise = Image.effect_noise((3000, 2000), 90 + index * 5).convert("RGB")
                noise.save(source, "PNG")
                items.append(PhotoItem(source, 3000, 2000, caption=f"FOTO {index + 1}"))
            layout = create_layout(items)

            legacy_path = root / "legacy.pdf"
            legacy_canvas = Canvas(str(legacy_path), pagesize=(layout.page_width, layout.page_height))
            for page in layout.pages:
                for placement in page.placements:
                    image = open_normalized(placement.item.path)
                    legacy_canvas.drawImage(
                        ImageReader(image),
                        placement.x,
                        layout.page_height - placement.y - placement.geometry.photo_height,
                        width=placement.geometry.photo_width,
                        height=placement.geometry.photo_height,
                    )
                legacy_canvas.showPage()
            legacy_canvas.save()

            optimized_path = root / "optimized.pdf"
            create_pdf(layout, optimized_path)
            legacy_size = legacy_path.stat().st_size
            optimized_size = optimized_path.stat().st_size
            print(f"PDF_SIZE_COMPARISON before={legacy_size} after={optimized_size}")
            self.assertLess(optimized_size, legacy_size * 0.25)


if __name__ == "__main__":
    unittest.main()
