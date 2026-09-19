package it.fotoalbum.spike.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import it.fotoalbum.spike.model.PhotoEntry
import java.io.File
import java.io.IOException
import java.util.UUID

data class ImportResult(val photos: List<PhotoEntry>, val rejectedCount: Int)

class PhotoCacheAdapter(private val context: Context) {
    private val inputDirectory = File(context.cacheDir, "photo_inputs")

    fun startFreshSession() {
        inputDirectory.deleteRecursively()
        check(inputDirectory.mkdirs() || inputDirectory.isDirectory)
    }

    fun importUris(uris: List<Uri>): ImportResult {
        inputDirectory.mkdirs()
        val photos = mutableListOf<PhotoEntry>()
        var rejected = 0
        for (uri in uris) {
            var destination: File? = null
            try {
                val extension = extensionForMime(context.contentResolver.getType(uri))
                if (extension == null) {
                    rejected += 1
                    continue
                }
                val id = UUID.randomUUID().toString()
                val outputFile = File(inputDirectory, "$id.$extension")
                destination = outputFile
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("ContentResolver non ha restituito uno stream")
                input.use { source -> outputFile.outputStream().use { source.copyTo(it) } }
                if (outputFile.length() == 0L) throw IOException("File vuoto")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(outputFile.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw IOException("Immagine non decodificabile")
                }
                photos += PhotoEntry(
                    id = id,
                    localPath = outputFile.absolutePath,
                    displayName = displayName(uri) ?: "Fotografia",
                )
            } catch (_: Exception) {
                destination?.delete()
                rejected += 1
            }
        }
        return ImportResult(photos, rejected)
    }

    fun delete(photo: PhotoEntry) {
        val file = File(photo.localPath)
        if (file.parentFile == inputDirectory) file.delete()
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }

    companion object {
        fun extensionForMime(mimeType: String?): String? = when (mimeType?.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            else -> null
        }
    }
}
