package org.photocardlibre.app.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

object ImportLimits {
    const val MAX_FILE_BYTES = 30L * 1024L * 1024L
    const val MAX_SESSION_BYTES = 300L * 1024L * 1024L
    const val MAX_SESSION_PHOTOS = 50
}

enum class ImportRejection {
    UNSUPPORTED_OR_INVALID,
    FILE_TOO_LARGE,
    IMAGE_TOO_LARGE,
    SESSION_BYTES_EXCEEDED,
    SESSION_COUNT_EXCEEDED,
}

enum class ImageValidation { VALID, INVALID, TOO_MANY_PIXELS }

sealed interface GuardedImportResult {
    data class Success(val byteCount: Long) : GuardedImportResult
    data class Rejected(val reason: ImportRejection) : GuardedImportResult
}

class PhotoImportGuard(
    private val maxFileBytes: Long = ImportLimits.MAX_FILE_BYTES,
    private val maxSessionBytes: Long = ImportLimits.MAX_SESSION_BYTES,
    private val maxSessionPhotos: Int = ImportLimits.MAX_SESSION_PHOTOS,
    private val deleteFile: (File) -> Boolean = { it.delete() },
    private val onCleanupFailure: () -> Unit = {},
) {
    private var importedBytes = 0L
    private var importedPhotos = 0

    init {
        require(maxFileBytes > 0 && maxSessionBytes > 0 && maxSessionPhotos > 0)
    }

    fun reset() {
        importedBytes = 0L
        importedPhotos = 0
    }

    fun importFile(
        destination: File,
        openSource: () -> InputStream?,
        validate: (File) -> ImageValidation,
    ): GuardedImportResult {
        if (importedPhotos >= maxSessionPhotos) {
            return GuardedImportResult.Rejected(ImportRejection.SESSION_COUNT_EXCEEDED)
        }
        val sessionBytesRemaining = maxSessionBytes - importedBytes
        if (sessionBytesRemaining <= 0L) {
            return GuardedImportResult.Rejected(ImportRejection.SESSION_BYTES_EXCEEDED)
        }
        val copyLimit = min(maxFileBytes, sessionBytesRemaining)
        val overflowReason = if (sessionBytesRemaining < maxFileBytes) {
            ImportRejection.SESSION_BYTES_EXCEEDED
        } else {
            ImportRejection.FILE_TOO_LARGE
        }

        return try {
            val byteCount = openSource()?.use { input ->
                destination.outputStream().use { output ->
                    copyBounded(input, output, copyLimit)
                }
            } ?: throw IllegalStateException("ContentResolver returned no stream")
            if (byteCount == 0L) throw IllegalStateException("Empty input")
            when (validate(destination)) {
                ImageValidation.VALID -> {
                    importedBytes += byteCount
                    importedPhotos += 1
                    GuardedImportResult.Success(byteCount)
                }
                ImageValidation.TOO_MANY_PIXELS -> rejectAndDelete(
                    destination,
                    ImportRejection.IMAGE_TOO_LARGE,
                )
                ImageValidation.INVALID -> rejectAndDelete(
                    destination,
                    ImportRejection.UNSUPPORTED_OR_INVALID,
                )
            }
        } catch (_: ByteLimitExceededException) {
            rejectAndDelete(destination, overflowReason)
        } catch (_: Exception) {
            rejectAndDelete(destination, ImportRejection.UNSUPPORTED_OR_INVALID)
        }
    }

    private fun rejectAndDelete(
        destination: File,
        reason: ImportRejection,
    ): GuardedImportResult.Rejected {
        val deleted = try {
            !destination.exists() || deleteFile(destination) || !destination.exists()
        } catch (_: Exception) {
            false
        }
        if (!deleted) onCleanupFailure()
        return GuardedImportResult.Rejected(reason)
    }

    companion object {
        private const val COPY_BUFFER_BYTES = 8 * 1024

        internal fun copyBounded(input: InputStream, output: OutputStream, limit: Long): Long {
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var copied = 0L
            while (true) {
                val remaining = limit - copied
                val requested = min(buffer.size.toLong(), remaining + 1L).toInt()
                val read = input.read(buffer, 0, requested)
                if (read < 0) return copied
                if (read == 0) continue
                if (read.toLong() > remaining) throw ByteLimitExceededException()
                output.write(buffer, 0, read)
                copied += read
            }
        }
    }
}

private class ByteLimitExceededException : Exception()
