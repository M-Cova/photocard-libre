package org.photocardlibre.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfImageSizeTest {
    @Test
    fun defaultIsFiveCentimeters() {
        assertEquals(PdfImageSize.CM_5, PdfImageSize.DEFAULT)
        assertEquals(PdfImageSize.CM_5, PdfImageSize.fromCentimeters(null))
        assertEquals(PdfImageSize.CM_5, PdfImageSize.fromCentimeters(99))
    }

    @Test
    fun allPhysicalPresetsAreSupported() {
        assertEquals(PdfImageSize.CM_3, PdfImageSize.fromCentimeters(3))
        assertEquals(PdfImageSize.CM_5, PdfImageSize.fromCentimeters(5))
        assertEquals(PdfImageSize.CM_7, PdfImageSize.fromCentimeters(7))
        assertEquals(PdfImageSize.CM_10, PdfImageSize.fromCentimeters(10))
    }
}
