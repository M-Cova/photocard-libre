package org.photocardlibre.app.settings

import java.util.Locale

enum class AppLanguage(val storageValue: String) {
    SYSTEM("system"),
    ITALIAN("it"),
    ENGLISH("en"),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromStorageValue(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT

        fun resolvedLocale(language: AppLanguage, systemLocale: Locale): Locale = when (language) {
            ITALIAN -> Locale.ITALIAN
            ENGLISH -> Locale.ENGLISH
            SYSTEM -> if (systemLocale.language == Locale.ITALIAN.language) {
                Locale.ITALIAN
            } else {
                Locale.ENGLISH
            }
        }
    }
}
