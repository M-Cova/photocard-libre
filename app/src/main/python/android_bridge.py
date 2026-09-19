"""Bridge JSON stabile tra il frontend Kotlin e il core Python."""

import json
from pathlib import Path
import traceback

from photo_album.images import ImageLoadError, load_photo
from photo_album.layout import create_layout
from photo_album.models import CaptionError, NormalizedCrop
from photo_album.rendering import create_pdf, render_preview_page


def render_album(items_json: str, output_directory: str, include_pdf: bool) -> str:
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

        layout = create_layout(items)
        output = Path(output_directory)
        output.mkdir(parents=True, exist_ok=True)
        for old_preview in output.glob("anteprima-*.png"):
            old_preview.unlink()

        preview_paths = []
        for page_index in range(len(layout.pages)):
            preview_path = output / f"anteprima-{page_index + 1}.png"
            render_preview_page(layout, page_index).save(preview_path, "PNG")
            preview_paths.append(str(preview_path))

        pdf_path = None
        if include_pdf:
            destination = output / "foto-album.pdf"
            create_pdf(layout, destination)
            if not destination.read_bytes().startswith(b"%PDF"):
                raise RuntimeError("Output PDF non valido")
            pdf_path = str(destination)

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
        return _failure(str(error), error)
    except Exception as error:
        return _failure("Errore durante la creazione del PDF.", error)


def _failure(user_error: str, error: Exception) -> str:
    traceback.print_exc()
    return json.dumps({
        "success": False,
        "user_error": user_error,
        "debug_error": f"{type(error).__name__}: {error}",
    })
