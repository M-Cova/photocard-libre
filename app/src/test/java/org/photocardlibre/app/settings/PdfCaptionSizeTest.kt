package org.photocardlibre.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfCaptionSizeTest {
    @Test
    fun defaultAndUnknownStoredValuesResolveToMedium() {
        assertEquals(PdfCaptionSize.MEDIUM, PdfCaptionSize.DEFAULT)
        assertEquals(PdfCaptionSize.MEDIUM, PdfCaptionSize.fromStorageValue(null))
        assertEquals(PdfCaptionSize.MEDIUM, PdfCaptionSize.fromStorageValue("unknown"))
    }

    @Test
    fun everySupportedValueRoundTripsThroughStorage() {
        PdfCaptionSize.entries.forEach { size ->
            assertEquals(size, PdfCaptionSize.fromStorageValue(size.storageValue))
        }
    }
}
