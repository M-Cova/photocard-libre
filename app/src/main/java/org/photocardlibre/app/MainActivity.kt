package org.photocardlibre.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import org.photocardlibre.app.ui.AlbumScreen
import org.photocardlibre.app.ui.theme.PhotoCardLibreTheme
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val albumViewModel: AlbumViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        // DataStore is the single source of truth. This small blocking read must happen before
        // Activity resources are created, otherwise the first frame can use the previous locale.
        val language = runBlocking { SettingsRepository(newBase).appLanguage.first() }
        val systemLocale = newBase.resources.configuration.locales[0]
        val locale = AppLanguage.resolvedLocale(language, systemLocale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoCardLibreTheme {
                AlbumScreen(albumViewModel)
            }
        }
    }
}
