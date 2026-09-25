package org.photocardlibre.app.storage

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrivateCacheManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun freshSessionRemovesBothPreviousCachesAndRecreatesDirectories() {
        val cache = temporaryFolder.newFolder("cache")
        val inputs = File(cache, "photo_inputs").apply { mkdirs() }
        val outputs = File(cache, "photo_output").apply { mkdirs() }
        File(inputs, "personal-photo.jpg").writeText("photo")
        File(outputs, "foto-album.pdf").writeText("pdf")

        val result = PrivateCacheManager(cache).startFreshSession()

        assertTrue(result)
        assertTrue(inputs.isDirectory)
        assertTrue(outputs.isDirectory)
        assertTrue(inputs.listFiles().isNullOrEmpty())
        assertTrue(outputs.listFiles().isNullOrEmpty())
    }

    @Test
    fun freshSessionNeverTouchesFilesOutsideTheTwoPrivateCacheDirectories() {
        val cache = temporaryFolder.newFolder("cache")
        val simulatedSavedPdf = temporaryFolder.newFile("saved-by-user.pdf").apply {
            writeText("saved")
        }
        val unrelatedCacheFile = File(cache, "unrelated.tmp").apply { writeText("keep") }

        assertTrue(PrivateCacheManager(cache).startFreshSession())

        assertTrue(simulatedSavedPdf.exists())
        assertTrue(unrelatedCacheFile.exists())
    }

    @Test
    fun deletionFailureIsNonFatalAndCanBeRetriedAtTheNextSession() {
        val cache = temporaryFolder.newFolder("cache")
        val inputs = File(cache, "photo_inputs").apply { mkdirs() }
        val outputs = File(cache, "photo_output").apply { mkdirs() }
        File(inputs, "leftover.jpg").writeText("photo")
        File(outputs, "leftover.pdf").writeText("pdf")
        val failures = mutableListOf<PrivateCacheArea>()
        val failingManager = PrivateCacheManager(
            cacheDirectory = cache,
            deleteTree = { false },
            onCleanupFailure = failures::add,
        )

        assertFalse(failingManager.startFreshSession())
        assertTrue(File(inputs, "leftover.jpg").exists())
        assertTrue(File(outputs, "leftover.pdf").exists())
        assertTrue(failures.containsAll(PrivateCacheArea.entries))

        assertTrue(PrivateCacheManager(cache).startFreshSession())
        assertTrue(inputs.listFiles().isNullOrEmpty())
        assertTrue(outputs.listFiles().isNullOrEmpty())
    }
}
