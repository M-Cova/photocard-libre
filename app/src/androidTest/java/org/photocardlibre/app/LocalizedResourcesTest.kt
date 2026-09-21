package org.photocardlibre.app

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedResourcesTest {
    @Test
    fun supportedLanguagesHaveLocalizedFundamentalUiStrings() {
        val base = ApplicationProvider.getApplicationContext<android.content.Context>()
        val titles = mapOf(
            "it" to "IMPOSTAZIONI",
            "en" to "SETTINGS",
            "es" to "AJUSTES",
            "de" to "EINSTELLUNGEN",
            "fr" to "PARAMÈTRES",
            "pt" to "DEFINIÇÕES",
        )
        titles.forEach { (code, expected) ->
            val localized = base.createConfigurationContext(
                Configuration(base.resources.configuration).apply {
                    setLocales(LocaleList(Locale.forLanguageTag(code)))
                },
            )
            assertEquals(expected, localized.getString(R.string.settings_title))
            listOf(
                R.string.action_add_photos,
                R.string.action_preview,
                R.string.action_create_pdf,
                R.string.edit_photo,
                R.string.selected_photo_heading,
                R.string.pdf_image_size_section,
                R.string.pdf_caption_size_section,
                R.string.app_description,
                R.string.open_source_description,
                R.string.app_info_title,
                R.string.privacy_label,
                R.string.privacy_description,
                R.string.credits_label,
                R.string.value_to_be_defined,
            ).forEach { resource ->
                assertTrue(localized.getString(resource).isNotBlank())
            }
        }
    }
}
