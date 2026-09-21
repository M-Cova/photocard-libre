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
    }

    @Test
    fun explicitLanguagesAlwaysResolveToTheirLocale() {
        assertEquals("it", AppLanguage.resolvedLocale(AppLanguage.ITALIAN, Locale.JAPANESE).language)
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.ENGLISH, Locale.ITALIAN).language)
    }

    @Test
    fun systemUsesItalianOnlyForItalianAndFallsBackToEnglish() {
        assertEquals("it", AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.ITALIAN).language)
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.ENGLISH).language)
        assertEquals("en", AppLanguage.resolvedLocale(AppLanguage.SYSTEM, Locale.FRENCH).language)
    }
}
