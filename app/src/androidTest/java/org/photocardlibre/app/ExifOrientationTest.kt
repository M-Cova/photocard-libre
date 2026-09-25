package org.photocardlibre.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.photocardlibre.app.model.CropRect
import org.photocardlibre.app.ui.cropBitmap
import org.photocardlibre.app.ui.decodeOrientedBitmap
import java.io.File
import java.util.UUID

class ExifOrientationTest {
    @Test
    fun jpegOrientation1KeepsPixelsAndDimensions() {
        assertJpegOrientation(
            ExifInterface.ORIENTATION_NORMAL,
            expectedWidth = SOURCE_WIDTH,
            expectedHeight = SOURCE_HEIGHT,
            expectedTopLeft = Color.RED,
        )
    }

    @Test
    fun jpegOrientation3RotatesPixelsBy180Degrees() {
        assertJpegOrientation(
            ExifInterface.ORIENTATION_ROTATE_180,
            expectedWidth = SOURCE_WIDTH,
            expectedHeight = SOURCE_HEIGHT,
            expectedTopLeft = Color.YELLOW,
        )
    }

    @Test
    fun jpegOrientation6RotatesPixelsAndDimensionsClockwise() {
        assertJpegOrientation(
            ExifInterface.ORIENTATION_ROTATE_90,
            expectedWidth = SOURCE_HEIGHT,
            expectedHeight = SOURCE_WIDTH,
            expectedTopLeft = Color.BLUE,
        )
    }

    @Test
    fun jpegOrientation8RotatesPixelsAndDimensionsCounterClockwise() {
        assertJpegOrientation(
            ExifInterface.ORIENTATION_ROTATE_270,
            expectedWidth = SOURCE_HEIGHT,
            expectedHeight = SOURCE_WIDTH,
            expectedTopLeft = Color.GREEN,
        )
    }

    @Test
    fun jpegWithoutExifUsesNormalOrientation() {
        withSyntheticImage("jpg", Bitmap.CompressFormat.JPEG) { file ->
            val decoded = requireNotNull(decodeOrientedBitmap(file.absolutePath))
            assertEquals(SOURCE_WIDTH, decoded.width)
            assertEquals(SOURCE_HEIGHT, decoded.height)
            assertColorNear(Color.RED, decoded.getPixel(QUARTER_WIDTH, QUARTER_HEIGHT))
            decoded.recycle()
        }
    }

    @Test
    fun malformedExifFallsBackWithoutCrashing() {
        withSyntheticImage("jpg", Bitmap.CompressFormat.JPEG) { file ->
            insertApp1(file, "Exif\u0000\u0000BROKEN".toByteArray(Charsets.ISO_8859_1))

            val decoded = decodeOrientedBitmap(file.absolutePath)

            assertNotNull(decoded)
            assertEquals(SOURCE_WIDTH, decoded!!.width)
            assertEquals(SOURCE_HEIGHT, decoded.height)
            decoded.recycle()
        }
    }

    @Test
    fun incompleteExifMetadataFallsBackWithoutCrashing() {
        withSyntheticImage("jpg", Bitmap.CompressFormat.JPEG) { file ->
            val incompleteTiff = byteArrayOf(
                'E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0, 0,
                'I'.code.toByte(), 'I'.code.toByte(), 42, 0, 8, 0, 0, 0,
            )
            insertApp1(file, incompleteTiff)

            val decoded = decodeOrientedBitmap(file.absolutePath)

            assertNotNull(decoded)
            assertEquals(SOURCE_WIDTH, decoded!!.width)
            assertEquals(SOURCE_HEIGHT, decoded.height)
            decoded.recycle()
        }
    }

    @Test
    fun validPngWithoutExifIsDecodedNormally() {
        withSyntheticImage("png", Bitmap.CompressFormat.PNG) { file ->
            val decoded = requireNotNull(decodeOrientedBitmap(file.absolutePath))
            assertEquals(SOURCE_WIDTH, decoded.width)
            assertEquals(SOURCE_HEIGHT, decoded.height)
            assertColorNear(Color.RED, decoded.getPixel(QUARTER_WIDTH, QUARTER_HEIGHT))
            decoded.recycle()
        }
    }

    @Test
    fun cropAndLocalPreviewUseExifOrientedCoordinates() {
        withSyntheticImage("jpg", Bitmap.CompressFormat.JPEG) { file ->
            writeOrientation(file, ExifInterface.ORIENTATION_ROTATE_90)
            val oriented = requireNotNull(decodeOrientedBitmap(file.absolutePath))

            val preview = cropBitmap(oriented, CropRect(0.0, 0.0, 0.5, 1.0))

            assertEquals(SOURCE_HEIGHT / 2, preview.width)
            assertEquals(SOURCE_WIDTH, preview.height)
            assertColorNear(Color.BLUE, preview.getPixel(preview.width / 2, QUARTER_WIDTH))
            assertColorNear(Color.YELLOW, preview.getPixel(preview.width / 2, SOURCE_WIDTH - QUARTER_WIDTH))
            preview.recycle()
            oriented.recycle()
        }
    }

    private fun assertJpegOrientation(
        orientation: Int,
        expectedWidth: Int,
        expectedHeight: Int,
        expectedTopLeft: Int,
    ) {
        withSyntheticImage("jpg", Bitmap.CompressFormat.JPEG) { file ->
            writeOrientation(file, orientation)

            val decoded = requireNotNull(decodeOrientedBitmap(file.absolutePath))

            assertEquals(expectedWidth, decoded.width)
            assertEquals(expectedHeight, decoded.height)
            assertColorNear(expectedTopLeft, decoded.getPixel(decoded.width / 4, decoded.height / 4))
            decoded.recycle()
        }
    }

    private fun writeOrientation(file: File, orientation: Int) {
        ExifInterface(file).run {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
    }

    private fun withSyntheticImage(
        extension: String,
        format: Bitmap.CompressFormat,
        block: (File) -> Unit,
    ) {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File(cache, "exif-${UUID.randomUUID()}.$extension")
        val bitmap = quadrantBitmap()
        try {
            file.outputStream().use { output ->
                assertTrue(bitmap.compress(format, 100, output))
            }
            block(file)
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    private fun quadrantBitmap(): Bitmap = Bitmap.createBitmap(
        SOURCE_WIDTH,
        SOURCE_HEIGHT,
        Bitmap.Config.ARGB_8888,
    ).apply {
        for (x in 0 until SOURCE_WIDTH) {
            for (y in 0 until SOURCE_HEIGHT) {
                val color = when {
                    x < SOURCE_WIDTH / 2 && y < SOURCE_HEIGHT / 2 -> Color.RED
                    x >= SOURCE_WIDTH / 2 && y < SOURCE_HEIGHT / 2 -> Color.GREEN
                    x < SOURCE_WIDTH / 2 -> Color.BLUE
                    else -> Color.YELLOW
                }
                setPixel(x, y, color)
            }
        }
    }

    private fun insertApp1(file: File, payload: ByteArray) {
        val original = file.readBytes()
        check(original.size >= 2 && original[0] == 0xff.toByte() && original[1] == 0xd8.toByte())
        val segmentLength = payload.size + 2
        val segment = byteArrayOf(
            0xff.toByte(),
            0xe1.toByte(),
            (segmentLength ushr 8).toByte(),
            segmentLength.toByte(),
        ) + payload
        file.writeBytes(original.copyOfRange(0, 2) + segment + original.copyOfRange(2, original.size))
    }

    private fun assertColorNear(expected: Int, actual: Int) {
        val tolerance = 24
        assertTrue(kotlin.math.abs(Color.red(expected) - Color.red(actual)) <= tolerance)
        assertTrue(kotlin.math.abs(Color.green(expected) - Color.green(actual)) <= tolerance)
        assertTrue(kotlin.math.abs(Color.blue(expected) - Color.blue(actual)) <= tolerance)
    }

    companion object {
        private const val SOURCE_WIDTH = 80
        private const val SOURCE_HEIGHT = 40
        private const val QUARTER_WIDTH = SOURCE_WIDTH / 4
        private const val QUARTER_HEIGHT = SOURCE_HEIGHT / 4
    }
}
