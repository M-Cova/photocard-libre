package org.photocardlibre.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        composeRule.onNodeWithTag("azioni_principali").assertIsDisplayed()
    }

    @Test
    fun settingsShowsMainOptions() {
        openSettings()
        listOf(
            "System",
            "Italiano",
            "English",
            "5 cm",
            "7 cm",
            "10 cm",
            "Small",
            "Medium",
            "Large",
            "Show the cutting border",
            "App info",
        ).forEach { label ->
            composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
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
        openSettings()
        composeRule.onNodeWithTag("language_italian").performClick()
        composeRule.waitUntil {
            runBlocking {
                SettingsRepository(composeRule.activity).appLanguage.first() == AppLanguage.ITALIAN
            }
        }
        composeRule.onNodeWithText("IMPOSTAZIONI").assertIsDisplayed()
        composeRule.onNodeWithTag("language_italian").assertIsSelected()
        composeRule.onNodeWithTag("apri_info_app").performScrollTo().performClick()
        composeRule.onNodeWithText("INFO SULL'APP").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("INFO SULL'APP").assertIsDisplayed()
        pressSystemBack()
        composeRule.onNodeWithTag("language_italian").assertIsSelected()

        composeRule.onNodeWithTag("language_english").performClick()
        composeRule.onNodeWithText("SETTINGS").assertIsDisplayed()
        composeRule.onNodeWithTag("language_english").assertIsSelected()
    }

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
    }

    private fun openInfo() {
        openSettings()
        composeRule.onNodeWithTag("apri_info_app").performScrollTo().performClick()
    }

    private fun pressSystemBack() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}
