package org.photocardlibre.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.photocardlibre.app.settings.MeasurementUnit
import org.photocardlibre.app.settings.PdfImageSize

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

    @Test
    fun changingDisplayUnitPreservesPhysicalSizeAndRenderedOutputs() {
        val centimeters = AlbumUiState(
            pdfImageSize = PdfImageSize.CM_5,
            measurementUnit = MeasurementUnit.CENTIMETERS,
            previewPaths = listOf("/cache/preview.png"),
            pdfPath = "/cache/album.pdf",
        )

        val inches = centimeters.copy(measurementUnit = MeasurementUnit.INCHES)

        assertEquals(PdfImageSize.CM_5, inches.pdfImageSize)
        assertEquals(centimeters.previewPaths, inches.previewPaths)
        assertEquals(centimeters.pdfPath, inches.pdfPath)
    }
}
