package org.photocardlibre.app.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun defaultAndUnknownStoredValuesUseSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.DEFAULT)
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageValue(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageValue("unsupported"))
        assertEquals(12, AppLanguage.entries.size)
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromStorageValue(language.storageValue))
        }
    }

    @Test
    fun explicitLanguagesAlwaysResolveToTheirLocale() {
        assertEquals("it", AppLanguage.resolvedLocale(AppLanguage.ITALIAN, Locale.forLanguageTag("ru")).language)
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.ENGLISH, Locale.ITALIAN).language)
        assertEquals("es", AppLanguage.resolvedLocale(AppLanguage.SPANISH, Locale.ITALIAN).language)
        assertEquals("de", AppLanguage.resolvedLocale(AppLanguage.GERMAN, Locale.ITALIAN).language)
        assertEquals("fr", AppLanguage.resolvedLocale(AppLanguage.FRENCH, Locale.ITALIAN).language)
        assertEquals("pt", AppLanguage.resolvedLocale(AppLanguage.PORTUGUESE, Locale.ITALIAN).language)
        assertEquals("ar", AppLanguage.resolvedLocale(AppLanguage.ARABIC, Locale.ITALIAN).language)
        assertEquals("zh-Hans", AppLanguage.resolvedLocale(AppLanguage.SIMPLIFIED_CHINESE, Locale.ITALIAN).toLanguageTag())
        assertEquals("ja", AppLanguage.resolvedLocale(AppLanguage.JAPANESE, Locale.ITALIAN).language)
        assertEquals("hi", AppLanguage.resolvedLocale(AppLanguage.HINDI, Locale.ITALIAN).language)
        assertEquals("id", AppLanguage.resolvedLocale(AppLanguage.INDONESIAN, Locale.ITALIAN).language)
    }

    @Test
    fun systemUsesPrimarySupportedLanguageAndFallsBackToEnglish() {
        listOf("it", "en", "es", "de", "fr", "pt", "ar", "zh", "ja", "hi", "id").forEach { code ->
            assertEquals(code, AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("$code-XX")).language)
        }
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("ru-RU")).language)
    }
}
