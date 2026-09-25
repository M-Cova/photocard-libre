package org.photocardlibre.app.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class MeasurementUnitTest {
    @Test
    fun defaultAndUnknownStorageValuesUseCentimeters() {
        assertEquals(MeasurementUnit.CENTIMETERS, MeasurementUnit.DEFAULT)
        assertEquals(MeasurementUnit.CENTIMETERS, MeasurementUnit.fromStorageValue(null))
        assertEquals(MeasurementUnit.CENTIMETERS, MeasurementUnit.fromStorageValue("unknown"))
    }

    @Test
    fun inchesUseThePhysicalCentimeterPresetAndAtMostTwoDecimals() {
        val expected = mapOf(3 to "1.18\"", 5 to "1.97\"", 7 to "2.76\"", 10 to "3.94\"")
        expected.forEach { (centimeters, label) ->
            assertEquals(label, MeasurementUnit.INCHES.format(centimeters, Locale.US))
            val decimalPart = label.substringAfter('.', "").substringBefore('"')
            assertFalse(decimalPart.length > 2)
        }
    }

    @Test
    fun formattingIsLocalizedWithoutChangingThePhysicalSelection() {
        val selectedSize = PdfImageSize.CM_5
        assertEquals("5 cm", MeasurementUnit.CENTIMETERS.format(selectedSize.centimeters, Locale.ITALY))
        assertEquals("1,97\"", MeasurementUnit.INCHES.format(selectedSize.centimeters, Locale.ITALY))
        assertSame(PdfImageSize.CM_5, selectedSize)
        assertEquals(5, selectedSize.centimeters)
    }
}
