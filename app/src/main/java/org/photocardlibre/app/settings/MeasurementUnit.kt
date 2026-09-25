package org.photocardlibre.app.settings

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

enum class MeasurementUnit(val storageValue: String) {
    CENTIMETERS("cm"),
    INCHES("in"),
    ;

    fun format(centimeters: Int, locale: Locale): String = when (this) {
        CENTIMETERS -> "$centimeters cm"
        INCHES -> {
            val formatter = NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 0
                isGroupingUsed = false
                roundingMode = RoundingMode.HALF_UP
            }
            "${formatter.format(centimeters / CENTIMETERS_PER_INCH)}\""
        }
    }

    companion object {
        val DEFAULT = CENTIMETERS
        const val CENTIMETERS_PER_INCH = 2.54

        fun fromStorageValue(value: String?): MeasurementUnit =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
