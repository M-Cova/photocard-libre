import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from PIL import Image

from photo_album.images import crop_image, load_photo, open_for_item, optimize_for_pdf
from photo_album.layout import create_layout
from photo_album.models import NormalizedCrop
from photo_album.rendering import create_pdf, render_preview_page


class CropCoreTests(unittest.TestCase):
    def test_original_means_no_crop(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "photo.png"
            Image.new("RGB", (1200, 800), "blue").save(path)
            item = load_photo(path, None)
            self.assertIsNone(item.crop)
            self.assertEqual((1200, 800), (item.pixel_width, item.pixel_height))

    def test_supported_crop_aspect_ratios(self):
        image = Image.new("RGB", (1600, 1200), "blue")
        crops = {
            "1:1": NormalizedCrop(0.125, 0.0, 0.75, 1.0),
            "4:3": NormalizedCrop(0.0, 0.0, 1.0, 1.0),
            "3:2": NormalizedCrop(0.0, 1.0 / 18.0, 1.0, 8.0 / 9.0),
            "16:9": NormalizedCrop(0.0, 0.125, 1.0, 0.75),
        }
        expected = {"1:1": 1.0, "4:3": 4 / 3, "3:2": 3 / 2, "16:9": 16 / 9}
        for label, crop in crops.items():
            with self.subTest(label=label):
                result = crop_image(image, crop)
                self.assertAlmostEqual(expected[label], result.width / result.height, delta=0.003)

    def test_crop_must_stay_inside_normalized_bounds(self):
        for values in (
            (-0.01, 0.0, 1.0, 1.0),
            (0.0, -0.01, 1.0, 1.0),
            (0.2, 0.0, 0.9, 1.0),
            (0.0, 0.2, 1.0, 0.9),
        ):
            with self.subTest(values=values), self.assertRaises(ValueError):
                NormalizedCrop(*values)

    def test_crop_is_applied_after_exif_orientation(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "rotated.jpg"
            image = Image.new("RGB", (120, 60), "red")
            exif = image.getexif()
            exif[274] = 6
            image.save(path, exif=exif)
            crop = NormalizedCrop(0.0, 0.0, 1.0, 0.5)

            item = load_photo(path, crop)

            self.assertEqual((60, 60), (item.pixel_width, item.pixel_height))

    def test_crop_is_applied_before_pdf_resize(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "large.png"
            destination = root / "optimized.jpg"
            Image.new("RGB", (1600, 900), "green").save(source)
            crop = NormalizedCrop(0.0, 0.0, 0.5, 1.0)

            optimize_for_pdf(source, destination, crop)

            with Image.open(destination) as optimized:
                self.assertEqual((667, 750), optimized.size)

    def test_layout_uses_cropped_aspect_ratio(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "wide.png"
            Image.new("RGB", (1600, 900), "green").save(path)
            item = load_photo(path, NormalizedCrop(0.21875, 0.0, 0.5625, 1.0))
            placement = create_layout([item]).pages[0].placements[0]
            self.assertAlmostEqual(1.0, item.aspect_ratio)
            self.assertAlmostEqual(
                1.0,
                placement.geometry.photo_width / placement.geometry.photo_height,
            )

    def test_preview_and_pdf_use_the_same_crop(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "photo.png"
            Image.new("RGB", (1200, 800), "purple").save(path)
            crop = NormalizedCrop(0.2, 0.1, 0.6, 0.8)
            item = load_photo(path, crop)
            layout = create_layout([item])

            with patch("photo_album.images.crop_image", wraps=crop_image) as crop_spy:
                render_preview_page(layout, 0)
                create_pdf(layout, root / "album.pdf")

            applied = [call.args[1] for call in crop_spy.call_args_list]
            self.assertEqual([crop, crop], applied)
            self.assertEqual(
                (item.pixel_width, item.pixel_height),
                open_for_item(item).size,
            )


if __name__ == "__main__":
    unittest.main()
