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
import org.junit.Test
import org.photocardlibre.app.settings.PdfImageSize
import org.photocardlibre.app.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SettingsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeOpensSettings() {
        openSettings()
        composeRule.onNodeWithTag("schermata_impostazioni").assertIsDisplayed()
        composeRule.onNodeWithText("IMPOSTAZIONI").assertIsDisplayed()
    }

    @Test
    fun settingsOpensInfo() {
        openInfo()
        composeRule.onNodeWithTag("schermata_info_app").assertIsDisplayed()
        composeRule.onNodeWithText("INFO SULL'APP").assertIsDisplayed()
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
            "Sistema",
            "Italiano",
            "English",
            "5 cm",
            "7 cm",
            "10 cm",
            "Piccola",
            "Media",
            "Grande",
            "Mostra il bordo di taglio",
            "Info sull'app",
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

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("Apri impostazioni").performClick()
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
