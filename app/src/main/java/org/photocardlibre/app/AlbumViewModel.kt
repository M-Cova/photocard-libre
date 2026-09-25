package org.photocardlibre.app

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.photocardlibre.app.export.PdfOutputConfig
import org.photocardlibre.app.model.AlbumState
import org.photocardlibre.app.model.CropRect
import org.photocardlibre.app.python.PythonAlbumBridge
import org.photocardlibre.app.storage.PhotoCacheAdapter
import org.photocardlibre.app.settings.PdfImageSize
import org.photocardlibre.app.settings.PdfCaptionSize
import org.photocardlibre.app.settings.SettingsRepository
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.MeasurementUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(
    @StringRes val resource: Int,
    val formatArgs: List<Any> = emptyList(),
)

data class AlbumUiState(
    val album: AlbumState = AlbumState(),
    @StringRes val busyMessage: Int? = null,
    val userMessage: UiMessage? = null,
    val previewPaths: List<String> = emptyList(),
    val previewVisible: Boolean = false,
    val pdfPath: String? = null,
    val pdfPageCount: Int = 0,
    val pdfGenerationId: Long = 0,
    val savedPdfUri: String? = null,
    val pdfSaving: Boolean = false,
    val pdfImageSize: PdfImageSize = PdfImageSize.DEFAULT,
    val pdfCaptionSize: PdfCaptionSize = PdfCaptionSize.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.DEFAULT,
    val measurementUnit: MeasurementUnit = MeasurementUnit.DEFAULT,
) {
    val hasSavedPdfActions: Boolean
        get() = !pdfSaving && !pdfPath.isNullOrBlank() && !savedPdfUri.isNullOrBlank()
}

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val cache = PhotoCacheAdapter(application)
    private val python = PythonAlbumBridge(application)
    private val settings = SettingsRepository(application)

    var state by mutableStateOf(AlbumUiState())
        private set

    init {
        cache.startFreshSession()
        viewModelScope.launch {
            settings.appLanguage.collect { language ->
                state = state.copy(appLanguage = language)
            }
        }
        viewModelScope.launch {
            settings.pdfImageSize.collect { savedSize ->
                state = state.copy(pdfImageSize = savedSize)
            }
        }
        viewModelScope.launch {
            settings.measurementUnit.collect { savedUnit ->
                state = state.copy(measurementUnit = savedUnit)
            }
        }
        viewModelScope.launch {
            settings.pdfCaptionSize.collect { savedSize ->
                state = state.copy(pdfCaptionSize = savedSize)
            }
        }
    }

    fun selectAppLanguage(language: AppLanguage, onSaved: () -> Unit) {
        if (language == state.appLanguage) return
        state = state.copy(appLanguage = language, busyMessage = null, userMessage = null)
        viewModelScope.launch {
            settings.setAppLanguage(language)
            onSaved()
        }
    }

    fun selectPdfImageSize(size: PdfImageSize) {
        if (size == state.pdfImageSize) return

        state = state.copy(
            pdfImageSize = size,
            previewPaths = emptyList(),
            pdfPath = null,
            savedPdfUri = null,
        )
        viewModelScope.launch { settings.setPdfImageSize(size) }
    }

    fun selectMeasurementUnit(unit: MeasurementUnit) {
        if (unit == state.measurementUnit) return
        state = state.copy(measurementUnit = unit)
        viewModelScope.launch { settings.setMeasurementUnit(unit) }
    }

    fun selectPdfCaptionSize(size: PdfCaptionSize) {
        if (size == state.pdfCaptionSize) return

        state = state.copy(
            pdfCaptionSize = size,
            previewPaths = emptyList(),
            pdfPath = null,
            savedPdfUri = null,
        )
        viewModelScope.launch { settings.setPdfCaptionSize(size) }
    }

    fun importUris(uris: List<Uri>, defaultDisplayName: String) {
        if (uris.isEmpty() || state.busyMessage != null || state.pdfSaving) return
        state = state.copy(busyMessage = R.string.message_importing_photos, userMessage = null)
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) { cache.importUris(uris, defaultDisplayName) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state = state.copy(
                    busyMessage = null,
                    userMessage = UiMessage(R.string.message_no_photos_opened),
                )
                return@launch
            }
            val message = when {
                result.photos.isEmpty() -> UiMessage(R.string.message_no_photos_opened)
                result.rejectedCount > 0 -> UiMessage(R.string.message_some_photos_not_opened)
                else -> null
            }
            state = state.copy(
                album = state.album.add(result.photos), busyMessage = null,
                userMessage = message,
                pdfPath = if (result.photos.isEmpty()) state.pdfPath else null,
                savedPdfUri = if (result.photos.isEmpty()) state.savedPdfUri else null,
                previewPaths = if (result.photos.isEmpty()) state.previewPaths else emptyList(),
            )
        }
    }

    fun select(id: String) { state = state.copy(album = state.album.select(id)) }

    fun updateCaption(value: String) {
        val change = state.album.updateCaption(value)
        if (change.state == state.album && change.error == null) return
        state = state.copy(
            album = change.state,
            userMessage = if (change.error != null) {
                UiMessage(R.string.message_caption_max_words)
            } else null,
            pdfPath = if (change.error == null) null else state.pdfPath,
            savedPdfUri = if (change.error == null) null else state.savedPdfUri,
            previewPaths = if (change.error == null) emptyList() else state.previewPaths,
        )
    }

    fun move(id: String, offset: Int) {
        val moved = state.album.move(id, offset)
        if (moved == state.album) return
        state = state.copy(
            album = moved, pdfPath = null, savedPdfUri = null,
            previewPaths = emptyList(),
        )
    }

    fun updateCrop(crop: CropRect?) {
        val updatedAlbum = state.album.updateSelectedCrop(crop)
        if (updatedAlbum == state.album) return
        state = state.copy(
            album = updatedAlbum,
            pdfPath = null,
            savedPdfUri = null,
            previewPaths = emptyList(),
        )
    }

    fun deleteSelected() {
        val change = state.album.deleteSelected()
        if (change.removed == null) return
        cache.delete(change.removed)
        state = state.copy(
            album = change.state, pdfPath = null, savedPdfUri = null,
            previewPaths = emptyList(),
        )
    }

    fun preview() = render(includePdf = false)
    fun createPdf() = render(includePdf = true)

    private fun render(includePdf: Boolean) {
        if (state.busyMessage != null || state.pdfSaving) return
        if (state.album.photos.isEmpty()) {
            state = state.copy(userMessage = UiMessage(R.string.message_add_photo_first))
            return
        }
        state = state.copy(
            busyMessage = if (includePdf) R.string.message_creating_pdf
            else R.string.message_creating_preview,
            userMessage = null,
        )
        val photos = state.album.photos
        val pdfImageSize = state.pdfImageSize
        val pdfCaptionSize = state.pdfCaptionSize
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                python.render(
                    photos,
                    includePdf,
                    pdfImageSize,
                    pdfCaptionSize,
                )
            }
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
                onFailure = {
                    state = state.copy(
                        busyMessage = null,
                        userMessage = UiMessage(R.string.message_pdf_creation_error),
                    )
                },
            )
        }
    }

    fun closePreview() { state = state.copy(previewVisible = false) }
    fun showMessage(@StringRes message: Int) {
        state = state.copy(userMessage = UiMessage(message))
    }

    fun onPdfSaved(uri: Uri, inDownloads: Boolean) {
        state = state.copy(
            savedPdfUri = uri.toString(),
            pdfSaving = false,
            userMessage = if (inDownloads) {
                UiMessage(
                    R.string.message_pdf_saved_downloads,
                    listOf(PdfOutputConfig.userVisibleDestination),
                )
            } else {
                UiMessage(R.string.message_pdf_saved_selected_location)
            },
        )
    }

    fun onPdfSaveCancelled() {
        state = state.copy(pdfSaving = false)
    }

    fun onPdfSaveFailed() {
        state = state.copy(
            pdfSaving = false,
            userMessage = UiMessage(R.string.message_pdf_save_failed),
        )
    }
}
