package org.photocardlibre.app.storage

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoImportGuardTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun productionLimitsMatchTheImportContract() {
        assertEquals(30L * 1024L * 1024L, ImportLimits.MAX_FILE_BYTES)
        assertEquals(300L * 1024L * 1024L, ImportLimits.MAX_SESSION_BYTES)
        assertEquals(50, ImportLimits.MAX_SESSION_PHOTOS)
    }

    @Test
    fun fileJustBelowByteLimitIsCopiedOnce() {
        val guard = testGuard(maxFileBytes = 10)
        val destination = newDestination("below.jpg")
        val content = ByteArray(9) { it.toByte() }

        val result = guard.importFile(
            destination,
            { ByteArrayInputStream(content) },
            { ImageValidation.VALID },
        )

        assertEquals(GuardedImportResult.Success(9), result)
        assertArrayEquals(content, destination.readBytes())
    }

    @Test
    fun fileJustAboveByteLimitIsRejectedAndPartialFileIsDeleted() {
        val guard = testGuard(maxFileBytes = 10)
        val destination = newDestination("above.jpg")
        var sourceClosed = false
        val source = object : ByteArrayInputStream(ByteArray(11)) {
            override fun close() {
                sourceClosed = true
                super.close()
            }
        }

        val result = guard.importFile(
            destination,
            { source },
            { ImageValidation.VALID },
        )

        assertEquals(
            GuardedImportResult.Rejected(ImportRejection.FILE_TOO_LARGE),
            result,
        )
        assertFalse(destination.exists())
        assertTrue(sourceClosed)
    }

    @Test
    fun sessionTotalAllowsExactLimitWithoutDamagingEarlierFile() {
        val guard = testGuard(maxFileBytes = 10, maxSessionBytes = 15)
        val first = newDestination("first.jpg")
        val second = newDestination("second.png")

        assertTrue(importBytes(guard, first, 8) is GuardedImportResult.Success)
        assertEquals(GuardedImportResult.Success(7), importBytes(guard, second, 7))
        assertTrue(first.exists())
        assertTrue(second.exists())
    }

    @Test
    fun sessionTotalRejectsOverflowAndKeepsEarlierFile() {
        val guard = testGuard(maxFileBytes = 10, maxSessionBytes = 15)
        val first = newDestination("first.jpg")
        val rejected = newDestination("rejected.png")

        assertTrue(importBytes(guard, first, 8) is GuardedImportResult.Success)
        assertEquals(
            GuardedImportResult.Rejected(ImportRejection.SESSION_BYTES_EXCEEDED),
            importBytes(guard, rejected, 8),
        )
        assertTrue(first.exists())
        assertFalse(rejected.exists())
    }

    @Test
    fun invalidOrOversizedPixelImageIsNotAcceptedAndIsDeleted() {
        for (validation in listOf(ImageValidation.INVALID, ImageValidation.TOO_MANY_PIXELS)) {
            val guard = testGuard()
            val destination = newDestination("rejected-$validation.jpg")
            val result = guard.importFile(
                destination,
                { ByteArrayInputStream(byteArrayOf(1)) },
                { validation },
            )

            assertTrue(result is GuardedImportResult.Rejected)
            assertFalse(destination.exists())
        }
    }

    @Test
    fun photoCountIsCumulativeForTheSessionAndResettable() {
        val guard = testGuard(maxSessionPhotos = 1)
        assertTrue(importBytes(guard, newDestination("one.jpg"), 1) is GuardedImportResult.Success)

        val rejected = newDestination("two.jpg")
        assertEquals(
            GuardedImportResult.Rejected(ImportRejection.SESSION_COUNT_EXCEEDED),
            importBytes(guard, rejected, 1),
        )
        assertFalse(rejected.exists())

        guard.reset()
        assertTrue(importBytes(guard, newDestination("after-reset.jpg"), 1) is GuardedImportResult.Success)
    }

    private fun testGuard(
        maxFileBytes: Long = 10,
        maxSessionBytes: Long = 100,
        maxSessionPhotos: Int = 50,
    ) = PhotoImportGuard(maxFileBytes, maxSessionBytes, maxSessionPhotos)

    private fun newDestination(name: String): File = File(temporaryFolder.root, name)

    private fun importBytes(
        guard: PhotoImportGuard,
        destination: File,
        count: Int,
    ): GuardedImportResult = guard.importFile(
        destination,
        { ByteArrayInputStream(ByteArray(count)) },
        { ImageValidation.VALID },
    )
}
