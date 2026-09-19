package it.fotoalbum.spike.model

import kotlin.math.abs

data class CropRect(
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double,
) {
    init {
        require(left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite())
        require(left >= 0.0 && top >= 0.0)
        require(width > 0.0 && height > 0.0)
        require(left + width <= 1.0 + EPSILON)
        require(top + height <= 1.0 + EPSILON)
    }

    companion object {
        private const val EPSILON = 1e-9
    }
}

enum class CropPreset(val label: String, val aspectRatio: Double?) {
    ORIGINAL("ORIGINALE", null),
    SQUARE("1:1", 1.0),
    FOUR_THREE("4:3", 4.0 / 3.0),
    SIXTEEN_NINE("16:9", 16.0 / 9.0),
    ;

    fun defaultRect(imageWidth: Int, imageHeight: Int): CropRect? {
        val target = aspectRatio ?: return null
        require(imageWidth > 0 && imageHeight > 0)
        val imageAspect = imageWidth.toDouble() / imageHeight
        return if (imageAspect >= target) {
            val width = target / imageAspect
            CropRect((1.0 - width) / 2.0, 0.0, width, 1.0)
        } else {
            val height = imageAspect / target
            CropRect(0.0, (1.0 - height) / 2.0, 1.0, height)
        }
    }

    companion object {
        fun matching(crop: CropRect?, imageWidth: Int, imageHeight: Int): CropPreset {
            if (crop == null) return ORIGINAL
            val croppedAspect = crop.width * imageWidth / (crop.height * imageHeight)
            return entries
                .filter { it.aspectRatio != null }
                .minByOrNull { abs(it.aspectRatio!! - croppedAspect) }
                ?.takeIf { abs(it.aspectRatio!! - croppedAspect) < 0.02 }
                ?: ORIGINAL
        }
    }
}

fun transformCrop(
    current: CropRect,
    maximum: CropRect,
    panFractionX: Double,
    panFractionY: Double,
    zoomChange: Double,
    maximumZoom: Double = 8.0,
): CropRect {
    val safeZoom = zoomChange.coerceIn(0.25, 4.0)
    val newWidth = (current.width / safeZoom)
        .coerceIn(maximum.width / maximumZoom, maximum.width)
    val newHeight = newWidth * maximum.height / maximum.width
    val centerX = (
        current.left + current.width / 2.0 - panFractionX * current.width
    ).coerceIn(newWidth / 2.0, 1.0 - newWidth / 2.0)
    val centerY = (
        current.top + current.height / 2.0 - panFractionY * current.height
    ).coerceIn(newHeight / 2.0, 1.0 - newHeight / 2.0)
    return CropRect(
        left = (centerX - newWidth / 2.0).coerceIn(0.0, 1.0 - newWidth),
        top = (centerY - newHeight / 2.0).coerceIn(0.0, 1.0 - newHeight),
        width = newWidth,
        height = newHeight,
    )
}
