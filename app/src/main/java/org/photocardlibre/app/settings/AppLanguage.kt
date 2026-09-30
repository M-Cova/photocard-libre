package org.photocardlibre.app.settings

import java.util.Locale

enum class AppLanguage(
    val storageValue: String,
    val nativeName: String?,
    val languageTag: String = storageValue,
) {
    SYSTEM("system", null, "en"),
    ITALIAN("it", "Italiano"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    PORTUGUESE("pt", "Português"),
    ARABIC("ar", "العربية"),
    SIMPLIFIED_CHINESE("zh-Hans", "简体中文"),
    JAPANESE("ja", "日本語"),
    HINDI("hi", "हिन्दी"),
    INDONESIAN("id", "Bahasa Indonesia"),
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

        fun systemLanguageNativeName(systemLocale: Locale): String =
            entries.firstOrNull {
                it != SYSTEM && Locale.forLanguageTag(it.languageTag).language == systemLocale.language
            }?.nativeName ?: systemLocale.getDisplayLanguage(systemLocale).ifBlank {
                systemLocale.toLanguageTag()
            }
    }
}
