package org.photocardlibre.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.AppLanguage

class LanguageScreenMappingTest {
    @Test
    fun everyLanguageMapsToItsNativeNameResource() {
        val expected = mapOf(
            AppLanguage.SYSTEM to R.string.language_system,
            AppLanguage.ITALIAN to R.string.language_italian,
            AppLanguage.ENGLISH to R.string.language_english,
            AppLanguage.SPANISH to R.string.language_spanish,
            AppLanguage.GERMAN to R.string.language_german,
            AppLanguage.FRENCH to R.string.language_french,
            AppLanguage.PORTUGUESE to R.string.language_portuguese,
            AppLanguage.ARABIC to R.string.language_arabic,
            AppLanguage.SIMPLIFIED_CHINESE to R.string.language_simplified_chinese,
            AppLanguage.JAPANESE to R.string.language_japanese,
            AppLanguage.HINDI to R.string.language_hindi,
            AppLanguage.INDONESIAN to R.string.language_indonesian,
        )

        assertEquals(AppLanguage.entries.toSet(), expected.keys)
        expected.forEach { (language, resource) ->
            assertEquals(resource, language.labelResource)
        }
    }
}
