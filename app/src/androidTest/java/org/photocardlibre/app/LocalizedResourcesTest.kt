package org.photocardlibre.app

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedResourcesTest {
    @Test
    fun italianAndEnglishResourcesAreAvailable() {
        val base = ApplicationProvider.getApplicationContext<android.content.Context>()
        val italian = base.createConfigurationContext(
            Configuration(base.resources.configuration).apply {
                setLocales(LocaleList(Locale.ITALIAN))
            },
        )
        val english = base.createConfigurationContext(
            Configuration(base.resources.configuration).apply {
                setLocales(LocaleList(Locale.ENGLISH))
            },
        )

        assertEquals("IMPOSTAZIONI", italian.getString(R.string.settings_title))
        assertEquals("INFO SULL'APP", italian.getString(R.string.app_info_title))
        assertEquals("SETTINGS", english.getString(R.string.settings_title))
        assertEquals("APP INFO", english.getString(R.string.app_info_title))
    }
}
