"""Apertura sicura e preparazione delle fotografie per anteprima e PDF."""

from pathlib import Path
from tempfile import TemporaryDirectory

from PIL import Image, ImageOps

from . import config
from .config import SUPPORTED_EXTENSIONS
from .models import NormalizedCrop, PhotoItem

SUPPORTED_IMAGE_FORMATS = ("JPEG", "PNG")


class ImageLoadError(ValueError):
    pass


def open_normalized(path: Path | str) -> Image.Image:
    try:
        with Image.open(path, formats=SUPPORTED_IMAGE_FORMATS) as source:
            if source.format not in SUPPORTED_IMAGE_FORMATS:
                raise ImageLoadError("Formato immagine non supportato. Usa JPEG o PNG.")
            image = ImageOps.exif_transpose(source)
            image.load()
            if image.mode not in ("RGB", "L"):
                background = Image.new("RGBA", image.size, "white")
                if image.mode == "RGBA":
                    background.alpha_composite(image)
                else:
                    background.alpha_composite(image.convert("RGBA"))
                image = background.convert("RGB")
            elif image.mode == "L":
                image = image.convert("RGB")
            else:
                image = image.copy()
            return image
    except Exception as exc:
        if isinstance(exc, ImageLoadError):
            raise
        raise ImageLoadError("Impossibile leggere l'immagine. Usa JPEG o PNG.") from exc


def validate_user_image(path: Path | str) -> None:
    """Decodifica completamente un input utente usando soltanto JPEG e PNG."""
    image = open_normalized(path)
    image.close()


def crop_image(image: Image.Image, crop: NormalizedCrop | None) -> Image.Image:
    if crop is None:
        return image
    left = max(0, min(image.width - 1, round(crop.left * image.width)))
    top = max(0, min(image.height - 1, round(crop.top * image.height)))
    right = max(left + 1, min(image.width, round((crop.left + crop.width) * image.width)))
    bottom = max(top + 1, min(image.height, round((crop.top + crop.height) * image.height)))
    return image.crop((left, top, right, bottom))


def open_cropped(path: Path | str, crop: NormalizedCrop | None = None) -> Image.Image:
    return crop_image(open_normalized(path), crop)


def open_for_item(item: PhotoItem) -> Image.Image:
    return open_cropped(item.path, item.crop)


def load_photo(path: Path | str, crop: NormalizedCrop | None = None) -> PhotoItem:
    path = Path(path)
    if path.suffix.lower() not in SUPPORTED_EXTENSIONS:
        raise ImageLoadError("Formato non supportato. Usa JPG, JPEG o PNG.")
    image = open_cropped(path, crop)
    return PhotoItem(
        path=path,
        pixel_width=image.width,
        pixel_height=image.height,
        crop=crop,
    )


def optimize_for_pdf(
    source_path: Path | str,
    destination: Path | str,
    crop: NormalizedCrop | None = None,
) -> Path:
    """Crea un JPEG adatto alla stampa senza alterare il file sorgente."""
    source_path = Path(source_path)
    destination = Path(destination)
    image = open_cropped(source_path, crop)
    image.thumbnail(
        (config.PDF_IMAGE_MAX_SIDE_PX, config.PDF_IMAGE_MAX_SIDE_PX),
        Image.Resampling.LANCZOS,
    )
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(
        destination,
        "JPEG",
        quality=config.PDF_IMAGE_JPEG_QUALITY,
        optimize=True,
        dpi=(300, 300),
    )
    return destination


class PdfImageCache:
    """Cache temporanea, valida soltanto durante una generazione PDF."""

    def __init__(self, cache_parent: Path | str):
        self._temporary_directory = TemporaryDirectory(
            prefix="foto-album-pdf-",
            dir=Path(cache_parent),
        )
        self._paths: dict[tuple[Path, NormalizedCrop | None], Path] = {}

    def __enter__(self) -> "PdfImageCache":
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> None:
        self._temporary_directory.cleanup()

    def get(self, source: PhotoItem | Path | str) -> Path:
        if isinstance(source, PhotoItem):
            source_path = source.path.resolve()
            crop = source.crop
        else:
            source_path = Path(source).resolve()
            crop = None
        key = (source_path, crop)
        optimized = self._paths.get(key)
        if optimized is None:
            optimized = Path(self._temporary_directory.name) / f"{len(self._paths):04d}.jpg"
            optimize_for_pdf(source_path, optimized, crop)
            self._paths[key] = optimized
        return optimized
