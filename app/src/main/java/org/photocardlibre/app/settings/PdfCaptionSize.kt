package org.photocardlibre.app.settings

enum class PdfCaptionSize(val storageValue: String) {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
    ;

    companion object {
        val DEFAULT = MEDIUM

        fun fromStorageValue(value: String?): PdfCaptionSize =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
