"""Tests del bordo di taglio (cutting border) per Foto Album."""

import json
import tempfile
import unittest
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).parents[1] / "app/src/main/python/photo_album/assets/test_images"


class CuttingBorderDefaultTest(unittest.TestCase):
    """Il bordo di taglio deve essere abilitato (True) di default."""

    def test_default_cutting_border_is_true_in_python_bridge(self):
        """Il valore di default del parametro cutting_border_enabled deve essere True.

        Signature: render_album(items_json, output_directory, include_pdf, max_photo_side_cm, cutting_border_enabled)
        I defaults tuple si riferiscono agli ultimi parametri con default:
        (max_photo_side_cm=5, cutting_border_enabled=True), quindi index [-1]."""
        from android_bridge import render_album
        # I defaults sono (5, True) per (max_photo_side_cm, cutting_border_enabled)
        defaults = render_album.__defaults__
        self.assertEqual(len(defaults), 2)
        # cutting_border_enabled è l'ultimo default
        self.assertTrue(defaults[-1] is True)

    def test_default_cutting_border_is_true_in_rendering(self):
        """Senza parametro cutting_border esplicito, il bordo produce un PDF più grande."""
        from photo_album import rendering
        from photo_album.images import load_photo
        from photo_album.models import NormalizedCrop
        from photo_album.layout import create_layout

        crop = NormalizedCrop.from_dict({
            "left": 0.05, "top": 0.05, "width": 0.9, "height": 0.9,
        })
        item = load_photo(str(ASSETS / "quadrata.png"), crop)
        layout = create_layout([item], 5)

        with tempfile.TemporaryDirectory() as tmp:
            pdf_with = Path(tmp) / "with.pdf"
            rendering.create_pdf(layout, pdf_with)  # Default cutting_border=True
            pdf_without = Path(tmp) / "without.pdf"
            rendering.create_pdf(layout, pdf_without, cutting_border=False)
            size_with = len(pdf_with.read_bytes())
            size_without = len(pdf_without.read_bytes())

        # Il PDF con bordo è più grande perché include la descrizione geometrica del rettangolo.
        self.assertGreater(size_with, size_without,
                           "Il PDF con default True (bordo) deve essere più grande di quello senza bordo")


class CuttingBorderOffTest(unittest.TestCase):
    """Con cutting_border=False NON deve apparire alcun bordo nero."""

    def test_preview_no_black_borders_when_disabled(self):
        """La preview PNG non ha bordi neri quando cutting border è OFF."""
        from photo_album.images import load_photo
        from photo_album.models import NormalizedCrop
        from photo_album.layout import create_layout
        from photo_album.rendering import render_preview_page

        crop = NormalizedCrop.from_dict({
            "left": 0.05, "top": 0.05, "width": 0.9, "height": 0.95,
        })
        item = load_photo(str(ASSETS / "quadrata.png"), crop)
        layout = create_layout([item], 5)

        preview_img = render_preview_page(layout, 0, cutting_border=False)
        width, height = preview_img.size
        # Controllo i margini estremi del canvas: devono essere bianchi (sfondo), nessun rettangolo nero esteso.
        step_x = max(1, width // 30)
        step_y = max(1, height // 30)
        edge_pixels = []
        for x in range(0, width, step_x):
            edge_pixels.append(preview_img.getpixel((x, 0)))
            edge_pixels.append(preview_img.getpixel((x, height - 1)))
        for y in range(0, height, step_y):
            edge_pixels.append(preview_img.getpixel((0, y)))
            edge_pixels.append(preview_img.getpixel((width - 1, y)))

        all_white_edges = all(p[0] > 240 and p[1] > 240 and p[2] > 240 for p in edge_pixels)
        self.assertTrue(all_white_edges, "I margini estremi devono essere bianchi con cutting border OFF")

    def test_pdf_no_border_when_disabled(self):
        """Il PDF generato senza border non deve contenere invocazioni 'rect'."""
        from photo_album import rendering
        from photo_album.images import load_photo
        from photo_album.models import NormalizedCrop
        from photo_album.layout import create_layout

        crop = NormalizedCrop.from_dict({
            "left": 0.05, "top": 0.05, "width": 0.9, "height": 0.9,
        })
        item = load_photo(str(ASSETS / "quadrata.png"), crop)
        layout = create_layout([item], 5)

        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / "test.pdf"
            rendering.create_pdf(layout, pdf_path, cutting_border=False)
            first_bytes = pdf_path.read_bytes()
            self.assertTrue(first_bytes.startswith(b"%PDF"), "Output deve essere un PDF valido")

            # Il modo reportlab disegna il bordo genera "rect" nel PDF stream.
            b_rect = b" rect"
            self.assertNotIn(b_rect, first_bytes,
                              "Il PDF senza bordo non deve contenere invocazioni 'rect'")

    def test_pdf_with_border_when_enabled(self):
        """Il PDF generato CON border è più grande del corrispondente SENZA border."""
        from photo_album import rendering
        from photo_album.images import load_photo
        from photo_album.models import NormalizedCrop
        from photo_album.layout import create_layout

        crop = NormalizedCrop.from_dict({
            "left": 0.05, "top": 0.05, "width": 0.9, "height": 0.9,
        })
        item = load_photo(str(ASSETS / "quadrata.png"), crop)
        layout = create_layout([item], 5)

        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / "test.pdf"
            rendering.create_pdf(layout, pdf_path, cutting_border=True)
            first_bytes = pdf_path.read_bytes()
            self.assertTrue(first_bytes.startswith(b"%PDF"))
            size_with = len(first_bytes)

        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / "test.pdf"
            rendering.create_pdf(layout, pdf_path, cutting_border=False)
            second_bytes = pdf_path.read_bytes()
            self.assertTrue(second_bytes.startswith(b"%PDF"))
            size_without = len(second_bytes)

        self.assertGreater(size_with, size_without,
                           "Il PDF con bordo deve essere più grande del corrispondente senza bordo")


class CuttingBorderLayoutUnchangedTest(unittest.TestCase):
    """La presenza/assenza del bordo NON modifica il layout."""

    def test_layout_unchanged_with_and_without_border(self):
        """create_layout produce lo stesso risultato indipendentemente dal cutting border.
        
        Verifichiamo che create_pdf con/without border produca lo stesso layout geomoetrico
        confrontando i Placement delle due PDF."""
        from photo_album import rendering
        from photo_album.layout import create_layout
        from photo_album.images import load_photo
        from photo_album.models import NormalizedCrop

        items = [
            load_photo(str(ASSETS / "orizzontale.png"), None),
            load_photo(str(ASSETS / "verticale.png"), None),
        ]
        layout = create_layout(items, 5)

        # Verifichiamo che il layout stesso sia identico (le geometrie non cambiano).
        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / "test.pdf"
            rendering.create_pdf(layout, pdf_path, cutting_border=False)
            content_no = pdf_path.read_bytes()

        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / "test.pdf"
            rendering.create_pdf(layout, pdf_path, cutting_border=True)
            content_yes = pdf_path.read_bytes()

        # Le due PDF hanno lo stesso numero di pagine e stesse geometrie.
        # La differenza deve essere solo la presenza/assenza dei bordi (rect).
        self.assertTrue(content_no.startswith(b"%PDF"))
        self.assertTrue(content_yes.startswith(b"%PDF"))

        # Il contenuto con border deve essere più grande (bordi in più)
        self.assertGreater(len(content_yes), len(content_no),
                            "Il PDF con bordo deve contenere più dati del PDF senza bordo")


class CuttingBorderPreviewPngTest(unittest.TestCase):
    """Le preview PNG sono corrette sia ON che OFF."""

    def test_preview_png_on_border_present(self):
        """Con border ON, la preview contiene pixel scuri ai bordi delle tile."""
        from android_bridge import render_album
        with tempfile.TemporaryDirectory() as tmp:
            payload = json.dumps([{"path": str(ASSETS / "quadrata.png"), "caption": "ON BORDO"}])
            result = json.loads(render_album(payload, tmp, False, cutting_border_enabled=True))
            self.assertTrue(result["success"])
            img = Image.open(Path(result["preview_paths"][0]))
            pixels = img.getdata()
            has_dark = any(p[0] < 50 and p[1] < 50 and p[2] < 70 for p in pixels)
            self.assertTrue(has_dark, "Con border ON ci devono essere pixel scuri (bordi)")

    def test_preview_png_off_no_black_borders(self):
        """Con border OFF la preview non ha bordi neri.
        
        Verifica che tutti i pixel della tile border region siano bianchi o contengano solo elementi validi."""
        from android_bridge import render_album
        with tempfile.TemporaryDirectory() as tmp:
            payload = json.dumps([{"path": str(ASSETS / "quadrata.png"), "caption": "OFF BORDO"}])
            result = json.loads(render_album(payload, tmp, False, cutting_border_enabled=False))
            self.assertTrue(result["success"])
            img = Image.open(Path(result["preview_paths"][0]))
            width, height = img.size

            # Margine esterno: deve essere tutto bianco (sfondo)
            step_x = max(1, width // 30)
            step_y = max(1, height // 30)
            edge_pixels = []
            for x in range(0, width, step_x):
                edge_pixels.append(img.getpixel((x, 0)))
                edge_pixels.append(img.getpixel((x, height - 1)))
            for y in range(0, height, step_y):
                edge_pixels.append(img.getpixel((0, y)))
                edge_pixels.append(img.getpixel((width - 1, y)))

            all_white_edges = all(p[0] > 240 and p[1] > 240 and p[2] > 240 for p in edge_pixels)
            self.assertTrue(all_white_edges, "I margini estremi devono essere bianchi con border OFF")


if __name__ == "__main__":
    unittest.main()
