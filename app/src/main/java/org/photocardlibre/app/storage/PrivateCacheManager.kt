package org.photocardlibre.app.storage

import java.io.File

internal enum class PrivateCacheArea(val directoryName: String) {
    PHOTO_INPUTS("photo_inputs"),
    PHOTO_OUTPUT("photo_output"),
}

/** Owns only PhotoCard Libre's two private, temporary cache directories. */
internal class PrivateCacheManager(
    cacheDirectory: File,
    private val deleteTree: (File) -> Boolean = { it.deleteRecursively() },
    private val createDirectory: (File) -> Boolean = { it.mkdirs() || it.isDirectory },
    private val onCleanupFailure: (PrivateCacheArea) -> Unit = {},
) {
    val inputDirectory = File(cacheDirectory, PrivateCacheArea.PHOTO_INPUTS.directoryName)
    val outputDirectory = File(cacheDirectory, PrivateCacheArea.PHOTO_OUTPUT.directoryName)

    /**
     * Clears leftovers from the previous process/session and leaves both cache directories usable.
     * Failure is deliberately non-fatal: a later app start will retry the cleanup.
     */
    fun startFreshSession(): Boolean {
        val results = listOf(
            PrivateCacheArea.PHOTO_INPUTS to inputDirectory,
            PrivateCacheArea.PHOTO_OUTPUT to outputDirectory,
        ).map { (area, directory) -> resetDirectory(area, directory) }
        return results.all { it }
    }

    private fun resetDirectory(area: PrivateCacheArea, directory: File): Boolean {
        val deleted = try {
            !directory.exists() || deleteTree(directory) || !directory.exists()
        } catch (_: Exception) {
            false
        }
        val ready = try {
            createDirectory(directory) && directory.isDirectory
        } catch (_: Exception) {
            false
        }
        if (!deleted || !ready) onCleanupFailure(area)
        return deleted && ready
    }
}
