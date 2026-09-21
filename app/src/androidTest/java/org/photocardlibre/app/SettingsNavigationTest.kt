package org.photocardlibre.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.PdfImageSize
import org.photocardlibre.app.settings.PdfCaptionSize
import org.photocardlibre.app.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SettingsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun useEnglish() {
        runBlocking {
            SettingsRepository(composeRule.activity).setAppLanguage(AppLanguage.ENGLISH)
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun homeOpensSettings() {
        openSettings()
        composeRule.onNodeWithTag("schermata_impostazioni").assertIsDisplayed()
        composeRule.onNodeWithText("SETTINGS").assertIsDisplayed()
    }

    @Test
    fun settingsOpensInfo() {
        openInfo()
        composeRule.onNodeWithTag("schermata_info_app").assertIsDisplayed()
        composeRule.onNodeWithText("APP INFO").assertIsDisplayed()
        composeRule.onNodeWithTag("photocard_libre_wordmark").assertIsDisplayed()
        composeRule.onNodeWithText("PhotoCard Libre").assertIsDisplayed()
        composeRule.onNodeWithText("Free app for creating printable photo cards with captions.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("PhotoCard Libre is free and open-source software.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Version").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(BuildConfig.VERSION_NAME).assertIsDisplayed()
        composeRule.onNodeWithText("Privacy").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Photos are processed locally on the device and are not sent to external servers.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("To be defined").assertCountEquals(5)
    }

    @Test
    fun systemBackFromInfoReturnsToSettings() {
        openInfo()
        pressSystemBack()
        composeRule.onNodeWithTag("schermata_impostazioni").assertIsDisplayed()
    }

    @Test
    fun systemBackFromSettingsReturnsToHome() {
        openSettings()
        pressSystemBack()
        composeRule.onNodeWithText("PhotoCard Libre").assertIsDisplayed()
        composeRule.onNodeWithTag("album_vuoto").assertIsDisplayed()
    }

    @Test
    fun settingsShowsMainOptions() {
        openSettings()
        listOf(
            "5 cm",
            "7 cm",
            "10 cm",
            "Small",
            "Medium",
            "Large",
            "App info",
        ).forEach { label ->
            composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithTag("apri_lingua").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("CUTTING BORDER").assertDoesNotExist()
        composeRule.onNodeWithText("Show the cutting border").assertDoesNotExist()
    }

    @Test
    fun languageScreenShowsAllOptionsAndSystemBackReturnsToSettings() {
        openLanguage()
        composeRule.onNodeWithTag("schermata_lingua").assertIsDisplayed()
        listOf("System", "Italiano", "English", "Español", "Deutsch", "Français", "Português")
            .forEach { label -> composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithTag("language_en").assertIsSelected()
        pressSystemBack()
        composeRule.onNodeWithTag("schermata_impostazioni").assertIsDisplayed()
    }

    @Test
    fun selectedPdfImageSizeRemainsSelectedAfterActivityRecreation() {
        openSettings()
        composeRule.onNodeWithText("7 cm").performScrollTo().performClick()
        composeRule.waitUntil {
            runBlocking {
                SettingsRepository(composeRule.activity).pdfImageSize.first() == PdfImageSize.CM_7
            }
        }

        composeRule.activityRule.scenario.recreate()
        openSettings()
        composeRule.onNodeWithTag("pdf_size_7").assertIsSelected()
    }

    @Test
    fun selectedCaptionSizeRemainsSelectedAfterActivityRecreation() {
        openSettings()
        composeRule.onNodeWithText("Large").performScrollTo().performClick()
        composeRule.waitUntil {
            runBlocking {
                SettingsRepository(composeRule.activity).pdfCaptionSize.first() ==
                    PdfCaptionSize.LARGE
            }
        }

        composeRule.activityRule.scenario.recreate()
        openSettings()
        composeRule.onNodeWithTag("caption_size_large").assertIsSelected()
    }

    @Test
    fun languageSelectionIsAppliedToSettingsAndInfoAndRestoredAfterRecreation() {
        openLanguage()
        composeRule.onNodeWithTag("language_it").performClick()
        composeRule.waitUntil {
            runBlocking {
                SettingsRepository(composeRule.activity).appLanguage.first() == AppLanguage.ITALIAN
            }
        }
        composeRule.onNodeWithText("LINGUA").assertIsDisplayed()
        composeRule.onNodeWithTag("language_it").assertIsSelected()
        pressSystemBack()
        composeRule.onNodeWithText("Italiano").assertIsDisplayed()
        composeRule.onNodeWithTag("apri_info_app").performScrollTo().performClick()
        composeRule.onNodeWithText("INFO SULL'APP").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("INFO SULL'APP").assertIsDisplayed()
        pressSystemBack()
        composeRule.onNodeWithText("Italiano").assertIsDisplayed()

        composeRule.onNodeWithTag("apri_lingua").performScrollTo().performClick()
        composeRule.onNodeWithTag("language_en").performClick()
        composeRule.onNodeWithText("LANGUAGE").assertIsDisplayed()
        composeRule.onNodeWithTag("language_en").assertIsSelected()
    }

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
    }

    private fun openInfo() {
        openSettings()
        composeRule.onNodeWithTag("apri_info_app").performScrollTo().performClick()
    }

    private fun openLanguage() {
        openSettings()
        composeRule.onNodeWithTag("apri_lingua").performClick()
    }

    private fun pressSystemBack() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}
