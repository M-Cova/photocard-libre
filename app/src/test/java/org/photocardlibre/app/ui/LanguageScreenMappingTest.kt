package org.photocardlibre.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.photocardlibre.app.R
import org.photocardlibre.app.settings.AppLanguage

class LanguageScreenMappingTest {
    @Test
    fun selectorContainsSystemFollowedByEveryExplicitLanguage() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.entries.first())
        assertEquals(11, AppLanguage.entries.count { it.nativeName != null })

        val expectedResources = listOf(
            R.string.language_system,
            R.string.language_italian,
            R.string.language_english,
            R.string.language_spanish,
            R.string.language_german,
            R.string.language_french,
            R.string.language_portuguese,
            R.string.language_arabic,
            R.string.language_simplified_chinese,
            R.string.language_japanese,
            R.string.language_hindi,
            R.string.language_indonesian,
        )
        assertEquals(expectedResources, AppLanguage.entries.map { it.labelResource })
    }
}
