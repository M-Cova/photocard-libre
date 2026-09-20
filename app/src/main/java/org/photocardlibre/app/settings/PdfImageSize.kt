package org.photocardlibre.app.settings

enum class PdfImageSize(val centimeters: Int) {
    CM_5(5),
    CM_7(7),
    CM_10(10),
    ;

    companion object {
        val DEFAULT = CM_5

        fun fromCentimeters(value: Int?): PdfImageSize =
            entries.firstOrNull { it.centimeters == value } ?: DEFAULT
    }
}
