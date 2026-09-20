package org.photocardlibre.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CropTest {
    @Test
    fun originalHasNoCropAndConfirmationCanRemovePreviousCrop() {
        assertNull(CropPreset.ORIGINAL.defaultRect(1600, 900))
        val state = AlbumState().add(
            listOf(PhotoEntry("1", "/cache/photo.jpg", "photo.jpg")),
        ).updateSelectedCrop(CropRect(0.1, 0.1, 0.8, 0.8))
        assertNull(state.updateSelectedCrop(null).selectedPhoto?.crop)
    }

    @Test
    fun presetsProduceTheRequestedPixelAspectRatios() {
        val expected = mapOf(
            CropPreset.SQUARE to 1.0,
            CropPreset.FOUR_THREE to 4.0 / 3.0,
            CropPreset.THREE_FOUR to 3.0 / 4.0,
        )
        assertEquals(
            listOf("ORIGINALE", "1:1", "4:3", "3:4"),
            CropPreset.entries.map { it.label },
        )
        expected.forEach { (preset, ratio) ->
            val crop = requireNotNull(preset.defaultRect(1600, 900))
            val actual = crop.width * 1600 / (crop.height * 900)
            assertEquals(ratio, actual, 1e-9)
        }
    }

    @Test
    fun extremePanAndZoomNeverExposeEmptyAreas() {
        val maximum = requireNotNull(CropPreset.SQUARE.defaultRect(1600, 900))
        var crop = maximum
        repeat(20) {
            crop = transformCrop(crop, maximum, 100.0, -100.0, 4.0)
        }
        assertTrue(crop.left >= 0.0)
        assertTrue(crop.top >= 0.0)
        assertTrue(crop.left + crop.width <= 1.0 + 1e-9)
        assertTrue(crop.top + crop.height <= 1.0 + 1e-9)

        crop = transformCrop(crop, maximum, -100.0, 100.0, 0.01)
        assertTrue(crop.left >= 0.0)
        assertTrue(crop.top >= 0.0)
        assertTrue(crop.left + crop.width <= 1.0 + 1e-9)
        assertTrue(crop.top + crop.height <= 1.0 + 1e-9)
    }
}
