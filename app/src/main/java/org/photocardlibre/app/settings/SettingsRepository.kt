package org.photocardlibre.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    val appLanguage: Flow<AppLanguage> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppLanguage.fromStorageValue(preferences[APP_LANGUAGE])
        }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.storageValue
        }
    }

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

    val measurementUnit: Flow<MeasurementUnit> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            MeasurementUnit.fromStorageValue(preferences[MEASUREMENT_UNIT])
        }

    suspend fun setMeasurementUnit(unit: MeasurementUnit) {
        dataStore.edit { preferences ->
            preferences[MEASUREMENT_UNIT] = unit.storageValue
        }
    }

    val pdfCaptionSize: Flow<PdfCaptionSize> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            PdfCaptionSize.fromStorageValue(preferences[PDF_CAPTION_SIZE])
        }

    suspend fun setPdfCaptionSize(size: PdfCaptionSize) {
        dataStore.edit { preferences ->
            preferences[PDF_CAPTION_SIZE] = size.storageValue
        }
    }

    private companion object {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val PDF_CAPTION_SIZE = stringPreferencesKey("pdf_caption_size")
        val PDF_IMAGE_SIZE_CM = intPreferencesKey("pdf_image_size_cm")
        val MEASUREMENT_UNIT = stringPreferencesKey("measurement_unit")
    }
}
