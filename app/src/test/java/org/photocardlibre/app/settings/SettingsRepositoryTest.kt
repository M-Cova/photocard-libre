package org.photocardlibre.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun languageDefaultsToSystemAndEverySelectionPersists() = runBlocking {
        AppLanguage.entries.forEach { selectedLanguage ->
            val preferencesFile = File(
                temporaryFolder.newFolder("language_${selectedLanguage.storageValue}"),
                "settings.preferences_pb",
            )
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val firstRepository = SettingsRepository(
                PreferenceDataStoreFactory.create(scope = firstScope) { preferencesFile },
            )
            assertEquals(AppLanguage.SYSTEM, firstRepository.appLanguage.first())
            firstRepository.setAppLanguage(selectedLanguage)
            assertEquals(selectedLanguage, firstRepository.appLanguage.first())
            firstScope.cancel()

            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val recreatedRepository = SettingsRepository(
                    PreferenceDataStoreFactory.create(scope = secondScope) { preferencesFile },
                )
                assertEquals(selectedLanguage, recreatedRepository.appLanguage.first())
            } finally {
                secondScope.cancel()
            }
        }
    }

    @Test
    fun changingLanguageDoesNotChangeOtherSettings() = runBlocking {
        val preferencesFile = File(temporaryFolder.root, "independent_settings.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = SettingsRepository(
                PreferenceDataStoreFactory.create(scope = scope) { preferencesFile },
            )
            repository.setPdfImageSize(PdfImageSize.CM_10)
            repository.setPdfCaptionSize(PdfCaptionSize.LARGE)
            repository.setAppLanguage(AppLanguage.ITALIAN)

            assertEquals(PdfImageSize.CM_10, repository.pdfImageSize.first())
            assertEquals(PdfCaptionSize.LARGE, repository.pdfCaptionSize.first())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun selectionPersistsWhenRepositoryIsRecreated() = runBlocking {
        val preferencesFile = File(temporaryFolder.root, "settings.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = PreferenceDataStoreFactory.create(
            scope = firstScope,
            produceFile = { preferencesFile },
        )
        val firstRepository = SettingsRepository(firstStore)

        assertEquals(PdfImageSize.CM_5, firstRepository.pdfImageSize.first())
        firstRepository.setPdfImageSize(PdfImageSize.CM_7)
        assertEquals(PdfImageSize.CM_7, firstRepository.pdfImageSize.first())
        firstScope.cancel()

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val secondStore = PreferenceDataStoreFactory.create(
                scope = secondScope,
                produceFile = { preferencesFile },
            )
            val secondRepository = SettingsRepository(secondStore)
            assertEquals(PdfImageSize.CM_7, secondRepository.pdfImageSize.first())
            secondRepository.setPdfImageSize(PdfImageSize.CM_10)
            assertEquals(PdfImageSize.CM_10, secondRepository.pdfImageSize.first())
        } finally {
            secondScope.cancel()
        }
    }

    @Test
    fun captionSizeDefaultsToMediumAndEverySelectionPersists() = runBlocking {
        PdfCaptionSize.entries.forEach { selectedSize ->
            val preferencesFile = File(
                temporaryFolder.newFolder(selectedSize.storageValue),
                "settings.preferences_pb",
            )
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val firstStore = PreferenceDataStoreFactory.create(
                scope = firstScope,
                produceFile = { preferencesFile },
            )
            val firstRepository = SettingsRepository(firstStore)

            assertEquals(PdfCaptionSize.MEDIUM, firstRepository.pdfCaptionSize.first())
            firstRepository.setPdfCaptionSize(selectedSize)
            assertEquals(selectedSize, firstRepository.pdfCaptionSize.first())
            firstScope.cancel()

            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val secondStore = PreferenceDataStoreFactory.create(
                    scope = secondScope,
                    produceFile = { preferencesFile },
                )
                val secondRepository = SettingsRepository(secondStore)
                assertEquals(selectedSize, secondRepository.pdfCaptionSize.first())
            } finally {
                secondScope.cancel()
            }
        }
    }
}
