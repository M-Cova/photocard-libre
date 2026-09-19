package it.fotoalbum.spike.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.fotoalbum.spike.model.CropPreset
import it.fotoalbum.spike.model.CropRect
import it.fotoalbum.spike.model.PhotoEntry
import it.fotoalbum.spike.model.transformCrop
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CropEditorScreen(
    photo: PhotoEntry,
    onBack: () -> Unit,
    onConfirm: (CropRect?) -> Unit,
    modifier: Modifier = Modifier,
    navigationBarInsets: WindowInsets = WindowInsets.navigationBars,
) {
    val bitmap = remember(photo.localPath) { decodeOrientedBitmap(photo.localPath) }
    if (bitmap == null) return
    val initialPreset = remember(photo.id) {
        CropPreset.matching(photo.crop, bitmap.width, bitmap.height)
    }
    var preset by remember(photo.id) { mutableStateOf(initialPreset) }
    var workingCrop by remember(photo.id) {
        mutableStateOf(if (initialPreset == CropPreset.ORIGINAL) null else photo.crop)
    }
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("editor_crop"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("MODIFICA FOTO") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("indietro_crop"),
                    ) {
                        Text("←", style = MaterialTheme.typography.headlineSmall)
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        navigationBarInsets.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    )
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                ) {
                    Button(
                        onClick = {
                            onConfirm(
                                if (preset == CropPreset.ORIGINAL) null else workingCrop,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("conferma_crop"),
                    ) {
                        Text("CONFERMA")
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CropPreset.entries) { option ->
                    if (option == preset) {
                        Button(onClick = {}) { Text(option.label) }
                    } else {
                        OutlinedButton(onClick = {
                            preset = option
                            workingCrop = option.defaultRect(bitmap.width, bitmap.height)
                        }) {
                            Text(option.label)
                        }
                    }
                }
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val frameAspect = preset.aspectRatio
                    ?: bitmap.width.toDouble() / bitmap.height
                val availableAspect = maxWidth.value / maxHeight.value
                val frameWidth = if (availableAspect > frameAspect) {
                    maxHeight * frameAspect.toFloat()
                } else {
                    maxWidth
                }
                val frameHeight = if (availableAspect > frameAspect) {
                    maxHeight
                } else {
                    maxWidth / frameAspect.toFloat()
                }
                CropFrame(
                    bitmap = bitmap,
                    crop = workingCrop,
                    gesturesEnabled = preset != CropPreset.ORIGINAL,
                    maximumCrop = preset.defaultRect(bitmap.width, bitmap.height),
                    onCropChange = { workingCrop = it },
                    modifier = Modifier.width(frameWidth).height(frameHeight),
                )
            }
        }
    }
}

@Composable
private fun CropFrame(
    bitmap: Bitmap,
    crop: CropRect?,
    gesturesEnabled: Boolean,
    maximumCrop: CropRect?,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier,
) {
    val visibleCrop = crop ?: FULL_IMAGE
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val currentCrop by rememberUpdatedState(visibleCrop)
    Canvas(
        modifier = modifier
            .background(Color.Black)
            .then(
                if (gesturesEnabled && maximumCrop != null) {
                    Modifier.pointerInput(bitmap, maximumCrop) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (size.width > 0 && size.height > 0) {
                                onCropChange(
                                    transformCrop(
                                        current = currentCrop,
                                        maximum = maximumCrop,
                                        panFractionX = (pan.x / size.width).toDouble(),
                                        panFractionY = (pan.y / size.height).toDouble(),
                                        zoomChange = zoom.toDouble(),
                                    ),
                                )
                            }
                        }
                    }
                } else {
                    Modifier
                },
            )
            .border(2.dp, Color.White),
    ) {
        val left = (visibleCrop.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (visibleCrop.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = ((visibleCrop.left + visibleCrop.width) * bitmap.width)
            .roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = ((visibleCrop.top + visibleCrop.height) * bitmap.height)
            .roundToInt().coerceIn(top + 1, bitmap.height)
        drawImage(
            image = image,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(right - left, bottom - top),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
        drawRect(Color.White, style = Stroke(width = 2.dp.toPx()))
    }
}

internal fun decodeOrientedBitmap(path: String, maximumSide: Int = 2048): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / sampleSize > maximumSide) {
        sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null
    val orientation = runCatching {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }
    if (matrix.isIdentity) return decoded
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

internal fun cropBitmap(bitmap: Bitmap, crop: CropRect?): Bitmap {
    if (crop == null) return bitmap
    val left = (crop.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
    val top = (crop.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
    val right = ((crop.left + crop.width) * bitmap.width)
        .roundToInt().coerceIn(left + 1, bitmap.width)
    val bottom = ((crop.top + crop.height) * bitmap.height)
        .roundToInt().coerceIn(top + 1, bitmap.height)
    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}

private val FULL_IMAGE = CropRect(0.0, 0.0, 1.0, 1.0)
