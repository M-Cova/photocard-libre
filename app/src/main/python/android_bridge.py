"""Bridge JSON stabile tra il frontend Kotlin e il core Python."""

import json
import logging
import os
import shutil
from pathlib import Path

from photo_album import config
from photo_album.images import ImageLoadError, load_photo
from photo_album.layout import create_layout
from photo_album.models import CaptionError, NormalizedCrop
from photo_album.rendering import create_pdf, render_preview_page


LOGGER = logging.getLogger("PhotoCardCache")


def render_album(
    items_json: str,
    output_directory: str,
    include_pdf: bool,
    max_photo_side_cm: int = config.DEFAULT_MAX_PHOTO_SIDE_CM,
    caption_size: str = config.DEFAULT_CAPTION_SIZE,
) -> str:
    staging = None
    try:
        raw_items = json.loads(items_json)
        if not raw_items:
            raise ValueError("Aggiungi almeno una fotografia.")

        items = []
        for raw_item in raw_items:
            crop = NormalizedCrop.from_dict(raw_item.get("crop"))
            item = load_photo(raw_item["path"], crop)
            item.set_caption(raw_item.get("caption", ""))
            items.append(item)

        layout = create_layout(items, int(max_photo_side_cm))
        output = Path(output_directory)
        output.mkdir(parents=True, exist_ok=True)
        staging = output / ".render-in-progress"
        if staging.exists():
            shutil.rmtree(staging)
        staging.mkdir()

        preview_paths = []
        for page_index in range(len(layout.pages)):
            preview_path = output / f"anteprima-{page_index + 1}.png"
            staged_preview = staging / preview_path.name
            render_preview_page(
                layout,
                page_index,
                caption_size=caption_size,
            ).save(staged_preview, "PNG")
            preview_paths.append(str(preview_path))

        pdf_path = None
        if include_pdf:
            destination = output / "foto-album.pdf"
            staged_pdf = staging / destination.name
            create_pdf(
                layout,
                staged_pdf,
                caption_size=caption_size,
            )
            if not staged_pdf.read_bytes().startswith(b"%PDF"):
                raise RuntimeError("Output PDF non valido")
            pdf_path = str(destination)

        current_previews = set(output.glob("anteprima-*.png"))
        published_previews = {Path(path) for path in preview_paths}
        for preview_path in published_previews:
            os.replace(staging / preview_path.name, preview_path)
        for old_preview in current_previews - published_previews:
            old_preview.unlink()
        if include_pdf:
            os.replace(staged_pdf, destination)
        shutil.rmtree(staging)
        staging = None

        return json.dumps({
            "success": True,
            "pdf_path": pdf_path,
            "preview_paths": preview_paths,
            "page_count": len(layout.pages),
        })
    except CaptionError as error:
        return _failure(str(error), error)
    except ImageLoadError as error:
        return _failure("Non è stato possibile aprire questa fotografia.", error)
    except ValueError as error:
        return _failure("Dati di input non validi.", error)
    except Exception as error:
        return _failure("Errore durante la creazione del PDF.", error)
    finally:
        if staging is not None and staging.exists():
            try:
                shutil.rmtree(staging)
            except OSError:
                # The next session cleanup is the durable retry after process/filesystem failures.
                LOGGER.warning("Private render staging cleanup incomplete; retrying next session")


def _failure(user_error: str, error: Exception) -> str:
    return json.dumps({
        "success": False,
        "user_error": user_error,
        "debug_error": type(error).__name__,
    })
