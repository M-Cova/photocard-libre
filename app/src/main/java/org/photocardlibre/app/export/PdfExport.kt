package org.photocardlibre.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import org.photocardlibre.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class SaveStrategy { MEDIA_STORE, STORAGE_ACCESS_FRAMEWORK }

sealed interface AutomaticSaveResult {
    data class Saved(val uri: Uri) : AutomaticSaveResult
    data object UseDocumentPicker : AutomaticSaveResult
}

object PdfOutputConfig {
    const val FILE_NAME_PREFIX = "PhotoCard"
    const val OUTPUT_FOLDER_NAME = "PhotoCard Libre"
    const val MAX_FILE_NAME_LENGTH = 120

    val userVisibleDestination: String
        get() = "Download/$OUTPUT_FOLDER_NAME"
}

object PdfExport {
    val USER_VISIBLE_DESTINATION: String
        get() = PdfOutputConfig.userVisibleDestination

    fun mediaStoreRelativePath(downloadDirectory: String): String =
        "$downloadDirectory/${PdfOutputConfig.OUTPUT_FOLDER_NAME}/"

    fun automaticFileName(
        now: Date = Date(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).run {
            this.timeZone = timeZone
            format(now)
        }
        return "${PdfOutputConfig.FILE_NAME_PREFIX}_$timestamp.pdf"
    }

    fun normalizeFileName(rawName: String): String? {
        var base = rawName.trim()
        if (base.endsWith(".pdf", ignoreCase = true)) base = base.dropLast(4).trimEnd()
        base = base.map { character ->
            when {
                character.isLetterOrDigit() -> character
                character == ' ' || character == '-' || character == '_' ||
                    character == '.' || character == '(' || character == ')' -> character
                else -> '_'
            }
        }.joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.')
        if (base.isEmpty() || base == "." || base == "..") return null
        val maximumBaseLength = PdfOutputConfig.MAX_FILE_NAME_LENGTH - PDF_EXTENSION.length
        base = base.take(maximumBaseLength).trimEnd(' ', '.')
        if (base.isEmpty()) return null
        return "$base$PDF_EXTENSION"
    }

    fun uniqueFileName(normalizedName: String, existingNames: Collection<String>): String {
        val existing = existingNames.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        if (normalizedName.lowercase(Locale.ROOT) !in existing) return normalizedName
        val stem = normalizedName.dropLast(PDF_EXTENSION.length)
        var index = 2
        while (true) {
            val suffix = " ($index)"
            val maximumStemLength =
                PdfOutputConfig.MAX_FILE_NAME_LENGTH - PDF_EXTENSION.length - suffix.length
            val candidate = "${stem.take(maximumStemLength).trimEnd()}$suffix$PDF_EXTENSION"
            if (candidate.lowercase(Locale.ROOT) !in existing) return candidate
            index += 1
        }
    }

    fun strategyForSdk(sdkInt: Int): SaveStrategy =
        if (sdkInt >= Build.VERSION_CODES.Q) SaveStrategy.MEDIA_STORE
        else SaveStrategy.STORAGE_ACCESS_FRAMEWORK

    fun saveAutomatically(
        context: Context,
        sourcePath: String,
        fileName: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Result<AutomaticSaveResult> = runCatching {
        if (
            strategyForSdk(sdkInt) == SaveStrategy.STORAGE_ACCESS_FRAMEWORK ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            return@runCatching AutomaticSaveResult.UseDocumentPicker
        }
        AutomaticSaveResult.Saved(saveWithMediaStore(context, sourcePath, fileName))
    }

    fun save(context: Context, sourcePath: String, destination: Uri): Result<Unit> = runCatching {
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Destinazione non scrivibile")
        File(sourcePath).inputStream().use { input -> output.use { input.copyTo(it) } }
    }

    fun open(context: Context, uri: Uri): Result<Unit> = runCatching {
        context.startActivity(
            Intent.createChooser(createOpenIntent(uri), context.getString(R.string.pdf_open_chooser_title)),
        )
    }

    fun createOpenIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun createShareIntent(context: Context, sourcePath: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(sourcePath),
        )
        return createShareIntent(uri)
    }

    fun createShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri(
                "${PdfOutputConfig.OUTPUT_FOLDER_NAME} PDF",
                uri,
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun share(context: Context, sourcePath: String): Result<Unit> = runCatching {
        context.startActivity(
            Intent.createChooser(
                createShareIntent(context, sourcePath),
                context.getString(R.string.pdf_share_chooser_title),
            ),
        )
    }

    fun share(context: Context, uri: Uri): Result<Unit> = runCatching {
        context.startActivity(
            Intent.createChooser(
                createShareIntent(uri),
                context.getString(R.string.pdf_share_chooser_title),
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(context: Context, sourcePath: String, fileName: String): Uri {
        val resolver = context.contentResolver
        val relativePath = mediaStoreRelativePath(Environment.DIRECTORY_DOWNLOADS)
        val finalFileName = uniqueFileName(
            normalizedName = normalizeFileName(fileName) ?: error("Nome file non valido"),
            existingNames = buildSet {
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads.DISPLAY_NAME),
                    "${MediaStore.Downloads.RELATIVE_PATH} = ?",
                    arrayOf(relativePath),
                    null,
                )?.use { cursor ->
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    while (cursor.moveToNext()) add(cursor.getString(nameColumn))
                }
            },
        )
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        var destination: Uri? = null
        try {
            destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore non ha creato il documento")
            val output = resolver.openOutputStream(destination)
                ?: error("MediaStore non ha aperto il documento")
            File(sourcePath).inputStream().use { input -> output.use { input.copyTo(it) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            check(resolver.update(destination, values, null, null) == 1) {
                "MediaStore non ha pubblicato il documento"
            }
            return destination
        } catch (error: Exception) {
            destination?.let { resolver.delete(it, null, null) }
            throw error
        }
    }

    private const val PDF_EXTENSION = ".pdf"
}
