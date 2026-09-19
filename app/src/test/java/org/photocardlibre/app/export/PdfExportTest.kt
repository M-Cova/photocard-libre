package org.photocardlibre.app.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

class PdfExportTest {
    @Test
    fun automaticNameUsesRequiredLocalPattern() {
        val utc = TimeZone.getTimeZone("UTC")
        val instant = GregorianCalendar(utc).apply {
            set(2026, 8, 17, 12, 55, 0)
            set(GregorianCalendar.MILLISECOND, 0)
        }.time
        assertEquals(
            "PhotoCard_2026-09-17_1255.pdf",
            PdfExport.automaticFileName(instant, utc),
        )
    }

    @Test
    fun mediaStoreDestinationIsUserVisibleDownloadFolder() {
        assertEquals("PhotoCard", PdfOutputConfig.FILE_NAME_PREFIX)
        assertEquals("PhotoCard Libre", PdfOutputConfig.OUTPUT_FOLDER_NAME)
        assertEquals(PdfOutputConfig.userVisibleDestination, PdfExport.USER_VISIBLE_DESTINATION)
        assertEquals("Download/PhotoCard Libre/", PdfExport.mediaStoreRelativePath("Download"))
    }

    @Test
    fun customNameKeepsSpacesAndGetsPdfExtension() {
        assertEquals("VACANZE MARE.pdf", PdfExport.normalizeFileName("VACANZE MARE"))
        assertEquals("Album 2026.pdf", PdfExport.normalizeFileName(" Album 2026.PDF "))
    }

    @Test
    fun emptyAndDotOnlyNamesAreRejected() {
        assertNull(PdfExport.normalizeFileName(""))
        assertNull(PdfExport.normalizeFileName("   "))
        assertNull(PdfExport.normalizeFileName("."))
        assertNull(PdfExport.normalizeFileName(".."))
    }

    @Test
    fun invalidCharactersAndPathSeparatorsAreReplaced() {
        assertEquals(
            "Foto_ estate_.pdf",
            PdfExport.normalizeFileName("Foto: estate?.pdf"),
        )
        assertEquals(
            "_Vacanze_Mare_2026.pdf",
            PdfExport.normalizeFileName("../Vacanze/Mare\\2026"),
        )
    }

    @Test
    fun excessiveNamesAreLimited() {
        val normalized = requireNotNull(PdfExport.normalizeFileName("A".repeat(500)))
        assertEquals(PdfOutputConfig.MAX_FILE_NAME_LENGTH, normalized.length)
        assertTrue(normalized.endsWith(".pdf"))
    }

    @Test
    fun duplicateNamesReceiveTheFirstAvailableNumericSuffix() {
        assertEquals(
            "VACANZE MARE (3).pdf",
            PdfExport.uniqueFileName(
                "VACANZE MARE.pdf",
                listOf("VACANZE MARE.pdf", "vacanze mare (2).PDF"),
            ),
        )
        assertEquals(
            "NUOVO.pdf",
            PdfExport.uniqueFileName("NUOVO.pdf", listOf("ALTRO.pdf")),
        )
    }

    @Test
    fun oldAndroidUsesStorageAccessFrameworkFallback() {
        assertEquals(SaveStrategy.STORAGE_ACCESS_FRAMEWORK, PdfExport.strategyForSdk(28))
        assertEquals(SaveStrategy.MEDIA_STORE, PdfExport.strategyForSdk(29))
        assertEquals(SaveStrategy.MEDIA_STORE, PdfExport.strategyForSdk(35))
    }
}
