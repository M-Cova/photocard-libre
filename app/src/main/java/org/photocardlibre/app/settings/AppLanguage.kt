package org.photocardlibre.app.settings

import java.util.Locale

enum class AppLanguage(val storageValue: String, val languageTag: String = storageValue) {
    SYSTEM("system", "en"),
    ITALIAN("it"),
    ENGLISH("en"),
    SPANISH("es"),
    GERMAN("de"),
    FRENCH("fr"),
    PORTUGUESE("pt"),
    ARABIC("ar"),
    SIMPLIFIED_CHINESE("zh-Hans"),
    JAPANESE("ja"),
    HINDI("hi"),
    INDONESIAN("id"),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromStorageValue(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT

        fun resolvedLocale(language: AppLanguage, systemLocale: Locale): Locale {
            if (language != SYSTEM) return Locale.forLanguageTag(language.languageTag)

            return entries.firstOrNull {
                it != SYSTEM && Locale.forLanguageTag(it.languageTag).language == systemLocale.language
            }?.let { Locale.forLanguageTag(it.languageTag) } ?: Locale.ENGLISH
        }
    }
}
