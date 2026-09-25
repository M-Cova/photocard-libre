import struct
import tempfile
import unittest
from pathlib import Path

from PIL import Image

from photo_album.images import ImageLoadError, load_photo, open_normalized, validate_user_image


def minimal_psd() -> bytes:
    """PSD RGB 1x1 valido, abbastanza piccolo da vivere come fixture sintetica."""
    header = b"8BPS" + struct.pack(">H6xHIIHH", 1, 3, 1, 1, 8, 3)
    empty_sections = struct.pack(">III", 0, 0, 0)
    return header + empty_sections + struct.pack(">H", 0) + bytes((255, 0, 0))


class ImageSecurityTests(unittest.TestCase):
    def test_valid_jpeg_is_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "photo.jpg"
            Image.new("RGB", (16, 12), "red").save(path, "JPEG")

            validate_user_image(path)
            self.assertEqual((16, 12), open_normalized(path).size)

    def test_valid_png_is_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "photo.png"
            Image.new("RGBA", (12, 16), (0, 0, 255, 128)).save(path, "PNG")

            validate_user_image(path)
            self.assertEqual((12, 16), open_normalized(path).size)

    def test_psd_renamed_as_jpeg_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "renamed.jpg"
            path.write_bytes(minimal_psd())
            with Image.open(path) as unrestricted:
                self.assertEqual("PSD", unrestricted.format)

            with self.assertRaises(ImageLoadError):
                validate_user_image(path)

    def test_non_image_renamed_as_png_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "notes.png"
            path.write_text("not an image", encoding="utf-8")

            with self.assertRaises(ImageLoadError):
                load_photo(path)

    def test_declared_jpeg_name_with_non_jpeg_content_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            # PhotoCacheAdapter gives image/jpeg content a .jpg cache name.
            path = Path(directory) / "mime-image-jpeg.jpg"
            path.write_bytes(b"content supplied as image/jpeg")

            with self.assertRaises(ImageLoadError):
                validate_user_image(path)

    def test_corrupt_jpeg_is_rejected_during_full_decode(self):
        with tempfile.TemporaryDirectory() as directory:
            valid = Path(directory) / "valid.jpg"
            corrupt = Path(directory) / "corrupt.jpg"
            Image.new("RGB", (64, 64), "green").save(valid, "JPEG")
            corrupt.write_bytes(valid.read_bytes()[:-32])

            with self.assertRaises(ImageLoadError):
                validate_user_image(corrupt)

    def test_other_decodable_formats_are_rejected(self):
        for image_format, suffix in (("BMP", ".bmp"), ("GIF", ".gif"), ("TIFF", ".tiff")):
            with self.subTest(image_format=image_format), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / f"photo{suffix}"
                Image.new("RGB", (8, 8), "white").save(path, image_format)
                with Image.open(path) as unrestricted:
                    self.assertEqual(image_format, unrestricted.format)

                with self.assertRaises(ImageLoadError):
                    open_normalized(path)


if __name__ == "__main__":
    unittest.main()
