package org.photocardlibre.app.model

import java.util.Locale

data class PhotoEntry(
    val id: String,
    val localPath: String,
    val displayName: String,
    val caption: String = "",
    val crop: CropRect? = null,
)

data class AlbumState(
    val photos: List<PhotoEntry> = emptyList(),
    val selectedId: String? = null,
) {
    val selectedPhoto: PhotoEntry?
        get() = photos.firstOrNull { it.id == selectedId }

    fun add(newPhotos: List<PhotoEntry>): AlbumState {
        if (newPhotos.isEmpty()) return this
        return copy(photos = photos + newPhotos)
    }

    fun select(id: String): AlbumState =
        if (photos.any { it.id == id }) copy(selectedId = id) else this

    fun updateCaption(rawText: String): CaptionChange {
        val selected = selectedPhoto ?: return CaptionChange(this, null)
        val normalized = rawText.uppercase(Locale.ROOT).replace(Regex("\\s+"), " ").trimStart()
        if (normalized.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size > 4) {
            return CaptionChange(this, "La didascalia può contenere al massimo 4 parole.")
        }
        val updated = photos.map { if (it.id == selected.id) it.copy(caption = normalized) else it }
        return CaptionChange(copy(photos = updated), null)
    }

    fun move(id: String, offset: Int): AlbumState {
        val current = photos.indexOfFirst { it.id == id }
        if (current < 0) return this
        val target = (current + offset).coerceIn(0, photos.lastIndex)
        if (target == current) return this
        val reordered = photos.toMutableList()
        val item = reordered.removeAt(current)
        reordered.add(target, item)
        return copy(photos = reordered)
    }

    fun updateSelectedCrop(crop: CropRect?): AlbumState {
        val selected = selectedPhoto ?: return this
        return copy(
            photos = photos.map { photo ->
                if (photo.id == selected.id) photo.copy(crop = crop) else photo
            },
        )
    }

    fun deleteSelected(): DeleteChange {
        val current = photos.indexOfFirst { it.id == selectedId }
        if (current < 0) return DeleteChange(this, null)
        val removed = photos[current]
        val remaining = photos.toMutableList().also { it.removeAt(current) }
        val nextSelected = when {
            remaining.isEmpty() -> null
            current < remaining.size -> remaining[current].id
            else -> remaining.last().id
        }
        return DeleteChange(copy(photos = remaining, selectedId = nextSelected), removed)
    }
}

data class CaptionChange(val state: AlbumState, val error: String?)
data class DeleteChange(val state: AlbumState, val removed: PhotoEntry?)
