package org.photocardlibre.app.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.chaquo.python.Python
import org.photocardlibre.app.model.PhotoEntry
import java.io.File
import java.util.UUID

data class ImportResult(
    val photos: List<PhotoEntry>,
    val rejections: List<ImportRejection>,
)

class PhotoCacheAdapter(private val context: Context) {
    private val privateCache = PrivateCacheManager(context.cacheDir) {
        Log.w(TAG, "Pulizia cache privata incompleta; nuovo tentativo al prossimo avvio")
    }
    private val inputDirectory = privateCache.inputDirectory
    private val importGuard = PhotoImportGuard(
        onCleanupFailure = {
            Log.w(TAG, "Pulizia di un'importazione incompleta non riuscita")
        },
    )

    fun startFreshSession() {
        privateCache.startFreshSession()
        importGuard.reset()
    }

    fun importUris(uris: List<Uri>, defaultDisplayName: String): ImportResult {
        inputDirectory.mkdirs()
        val photos = mutableListOf<PhotoEntry>()
        val rejections = mutableListOf<ImportRejection>()
        for (uri in uris) {
            try {
                val extension = extensionForMime(context.contentResolver.getType(uri))
                if (extension == null) {
                    rejections += ImportRejection.UNSUPPORTED_OR_INVALID
                    continue
                }
                val id = UUID.randomUUID().toString()
                val resolvedDisplayName = displayName(uri) ?: defaultDisplayName
                val outputFile = File(inputDirectory, "$id.$extension")
                when (val result = importGuard.importFile(
                    destination = outputFile,
                    openSource = { context.contentResolver.openInputStream(uri) },
                    validate = ::validateImage,
                )) {
                    is GuardedImportResult.Success -> photos += PhotoEntry(
                        id = id,
                        localPath = outputFile.absolutePath,
                        displayName = resolvedDisplayName,
                    )
                    is GuardedImportResult.Rejected -> rejections += result.reason
                }
            } catch (_: Exception) {
                rejections += ImportRejection.UNSUPPORTED_OR_INVALID
            }
        }
        return ImportResult(photos, rejections)
    }

    private fun validateImage(file: File): ImageValidation = when (
        Python.getInstance()
            .getModule("photo_album.images")
            .callAttr("validate_user_image_for_import", file.absolutePath)
            .toString()
    ) {
        "valid" -> ImageValidation.VALID
        "too_many_pixels" -> ImageValidation.TOO_MANY_PIXELS
        else -> ImageValidation.INVALID
    }

    fun delete(photo: PhotoEntry) {
        val file = File(photo.localPath)
        if (
            file.parentFile == inputDirectory &&
            file.exists() &&
            !file.delete() &&
            file.exists()
        ) {
            Log.w(TAG, "Pulizia di una fotografia temporanea non riuscita")
        }
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }

    companion object {
        private const val TAG = "PhotoCardCache"

        fun extensionForMime(mimeType: String?): String? = when (mimeType?.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            else -> null
        }
    }
}
