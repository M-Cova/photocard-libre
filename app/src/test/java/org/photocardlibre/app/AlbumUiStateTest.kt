package org.photocardlibre.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumUiStateTest {
    @Test
    fun savedPdfActionsRequireGeneratedAndSuccessfullySavedPdf() {
        assertFalse(AlbumUiState().hasSavedPdfActions)
        assertFalse(AlbumUiState(pdfPath = "/cache/album.pdf").hasSavedPdfActions)
        assertFalse(
            AlbumUiState(
                pdfPath = "/cache/album.pdf",
                savedPdfUri = "content://media/external/downloads/42",
                pdfSaving = true,
            ).hasSavedPdfActions,
        )
        assertTrue(
            AlbumUiState(
                pdfPath = "/cache/album.pdf",
                savedPdfUri = "content://media/external/downloads/42",
                pdfSaving = false,
            ).hasSavedPdfActions,
        )
    }
}
