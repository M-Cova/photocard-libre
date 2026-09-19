package org.photocardlibre.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.photocardlibre.app.export.PdfOutputConfig
import org.photocardlibre.app.model.AlbumState
import org.photocardlibre.app.model.CropRect
import org.photocardlibre.app.python.AlbumRenderException
import org.photocardlibre.app.python.PythonAlbumBridge
import org.photocardlibre.app.storage.PhotoCacheAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumUiState(
    val album: AlbumState = AlbumState(),
    val busyMessage: String? = null,
    val userMessage: String? = null,
    val previewPaths: List<String> = emptyList(),
    val previewVisible: Boolean = false,
    val pdfPath: String? = null,
    val pdfPageCount: Int = 0,
    val pdfGenerationId: Long = 0,
    val savedPdfUri: String? = null,
    val pdfSaving: Boolean = false,
) {
    val hasSavedPdfActions: Boolean
        get() = !pdfSaving && !pdfPath.isNullOrBlank() && !savedPdfUri.isNullOrBlank()
}

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val cache = PhotoCacheAdapter(application)
    private val python = PythonAlbumBridge(application)

    var state by mutableStateOf(AlbumUiState())
        private set

    init { cache.startFreshSession() }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        state = state.copy(busyMessage = "Importazione fotografie…", userMessage = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { cache.importUris(uris) }
            val message = when {
                result.photos.isEmpty() -> "Non è stato possibile aprire le fotografie selezionate. Usa JPEG o PNG."
                result.rejectedCount > 0 -> "Alcune fotografie non sono state aperte. Usa JPEG o PNG."
                else -> null
            }
            state = state.copy(
                album = state.album.add(result.photos), busyMessage = null,
                userMessage = message, pdfPath = null, savedPdfUri = null,
            )
        }
    }

    fun select(id: String) { state = state.copy(album = state.album.select(id)) }

    fun updateCaption(value: String) {
        val change = state.album.updateCaption(value)
        state = state.copy(
            album = change.state, userMessage = change.error,
            pdfPath = null, savedPdfUri = null,
        )
    }

    fun move(offset: Int) {
        state = state.copy(
            album = state.album.moveSelected(offset), pdfPath = null, savedPdfUri = null,
        )
    }

    fun updateCrop(crop: CropRect?) {
        state = state.copy(
            album = state.album.updateSelectedCrop(crop),
            pdfPath = null,
            savedPdfUri = null,
            previewPaths = emptyList(),
        )
    }

    fun deleteSelected() {
        val change = state.album.deleteSelected()
        change.removed?.let(cache::delete)
        state = state.copy(album = change.state, pdfPath = null, savedPdfUri = null)
    }

    fun preview() = render(includePdf = false)
    fun createPdf() = render(includePdf = true)

    private fun render(includePdf: Boolean) {
        if (state.album.photos.isEmpty()) {
            state = state.copy(userMessage = "Aggiungi almeno una fotografia.")
            return
        }
        state = state.copy(
            busyMessage = if (includePdf) "Creazione PDF…" else "Creazione anteprima…",
            userMessage = null,
        )
        val photos = state.album.photos
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { python.render(photos, includePdf) }
            result.fold(
                onSuccess = { rendered ->
                    state = state.copy(
                        busyMessage = null,
                        previewPaths = rendered.previewPaths,
                        previewVisible = !includePdf,
                        pdfPath = rendered.pdfPath ?: state.pdfPath,
                        pdfPageCount = rendered.pageCount,
                        pdfGenerationId = if (includePdf) state.pdfGenerationId + 1
                            else state.pdfGenerationId,
                        savedPdfUri = if (includePdf) null else state.savedPdfUri,
                        pdfSaving = includePdf,
                        userMessage = null,
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        busyMessage = null,
                        userMessage = if (error is AlbumRenderException) error.message
                            else "Errore durante la creazione del PDF.",
                    )
                },
            )
        }
    }

    fun closePreview() { state = state.copy(previewVisible = false) }
    fun showMessage(message: String?) { state = state.copy(userMessage = message) }

    fun onPdfSaved(uri: Uri, inDownloads: Boolean) {
        state = state.copy(
            savedPdfUri = uri.toString(),
            pdfSaving = false,
            userMessage = if (inDownloads) {
                "PDF salvato in ${PdfOutputConfig.userVisibleDestination}"
            } else {
                "PDF salvato nella posizione scelta."
            },
        )
    }

    fun onPdfSaveCancelled() {
        state = state.copy(pdfSaving = false)
    }

    fun onPdfSaveFailed() {
        state = state.copy(
            pdfSaving = false,
            userMessage = "Il PDF è stato creato ma non è stato possibile salvarlo.",
        )
    }
}
