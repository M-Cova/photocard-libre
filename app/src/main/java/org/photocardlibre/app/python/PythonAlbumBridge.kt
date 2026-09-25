package org.photocardlibre.app.python

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import org.photocardlibre.app.BuildConfig
import org.photocardlibre.app.model.PhotoEntry
import org.photocardlibre.app.settings.PdfImageSize
import org.photocardlibre.app.settings.PdfCaptionSize
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RenderResult(val pdfPath: String?, val previewPaths: List<String>, val pageCount: Int)

class PythonAlbumBridge(context: Context) {
    private val outputDirectory = File(context.cacheDir, "photo_output")

    fun render(
        photos: List<PhotoEntry>,
        includePdf: Boolean,
        pdfImageSize: PdfImageSize,
        pdfCaptionSize: PdfCaptionSize,
    ): Result<RenderResult> = captureBridgeResult {
        outputDirectory.mkdirs()
        val payload = JSONArray().apply {
            photos.forEach { photo ->
                put(JSONObject().apply {
                    put("path", photo.localPath)
                    put("caption", photo.caption.trim())
                    photo.crop?.let { crop ->
                        put("crop", JSONObject().apply {
                            put("left", crop.left)
                            put("top", crop.top)
                            put("width", crop.width)
                            put("height", crop.height)
                        })
                    }
                })
            }
        }
        val rawResult = Python.getInstance()
            .getModule("android_bridge")
            .callAttr(
                "render_album",
                payload.toString(),
                outputDirectory.absolutePath,
                includePdf,
                pdfImageSize.centimeters,
                pdfCaptionSize.storageValue,
            )
            .toString()
        val json = JSONObject(rawResult)
        if (!json.getBoolean("success")) {
            if (BuildConfig.DEBUG) {
                pythonFailureLogMessage(json.optString("debug_error"), isDebug = true)?.let {
                    Log.e(TAG, it)
                }
            }
            throw AlbumRenderException(json.getString("user_error"))
        }
        val previews = json.getJSONArray("preview_paths")
        RenderResult(
            pdfPath = json.optString("pdf_path").takeIf { it.isNotEmpty() && it != "null" },
            previewPaths = List(previews.length()) { index -> previews.getString(index) },
            pageCount = json.getInt("page_count"),
        )
    }.onFailure { error ->
        if (BuildConfig.DEBUG) {
            val log = bridgeFailureLog(error, isDebug = true)
            Log.e(TAG, log.message, checkNotNull(log.throwable))
        } else {
            val log = bridgeFailureLog(error, isDebug = false)
            Log.e(TAG, log.message)
        }
    }

    companion object { private const val TAG = "FotoAlbumPython" }
}

class AlbumRenderException(message: String) : Exception(message)

internal fun <T> captureBridgeResult(operation: () -> T): Result<T> = runCatching(operation)

internal data class BridgeFailureLog(val message: String, val throwable: Throwable?)

internal fun bridgeFailureLog(error: Throwable, isDebug: Boolean): BridgeFailureLog =
    BridgeFailureLog(
        message = GENERIC_BRIDGE_FAILURE,
        throwable = error.takeIf { isDebug },
    )

internal fun pythonFailureLogMessage(errorType: String, isDebug: Boolean): String? =
    errorType.takeIf { isDebug && it.isNotBlank() }
        ?.let { "$GENERIC_BRIDGE_FAILURE ($it)" }

private const val GENERIC_BRIDGE_FAILURE = "Python bridge operation failed"
