package org.photocardlibre.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.settingsDataStore)

    val pdfImageSize: Flow<PdfImageSize> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            PdfImageSize.fromCentimeters(preferences[PDF_IMAGE_SIZE_CM])
        }

    suspend fun setPdfImageSize(size: PdfImageSize) {
        dataStore.edit { preferences ->
            preferences[PDF_IMAGE_SIZE_CM] = size.centimeters
        }
    }

    private companion object {
        val PDF_IMAGE_SIZE_CM = intPreferencesKey("pdf_image_size_cm")
    }
}
