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
        assertEquals(7, AppLanguage.entries.size)
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromStorageValue(language.storageValue))
        }
    }

    @Test
    fun explicitLanguagesAlwaysResolveToTheirLocale() {
        assertEquals("it", AppLanguage.resolvedLocale(AppLanguage.ITALIAN, Locale.JAPANESE).language)
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.ENGLISH, Locale.ITALIAN).language)
        assertEquals("es", AppLanguage.resolvedLocale(AppLanguage.SPANISH, Locale.ITALIAN).language)
        assertEquals("de", AppLanguage.resolvedLocale(AppLanguage.GERMAN, Locale.ITALIAN).language)
        assertEquals("fr", AppLanguage.resolvedLocale(AppLanguage.FRENCH, Locale.ITALIAN).language)
        assertEquals("pt", AppLanguage.resolvedLocale(AppLanguage.PORTUGUESE, Locale.ITALIAN).language)
    }

    @Test
    fun systemUsesPrimarySupportedLanguageAndFallsBackToEnglish() {
        listOf("it", "en", "es", "de", "fr", "pt").forEach { code ->
            assertEquals(code, AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("$code-XX")).language)
        }
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.JAPANESE).language)
    }
}
