package org.photocardlibre.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test
import org.photocardlibre.app.ui.SavedPdfActions
import org.photocardlibre.app.ui.PdfNameDialog
import org.junit.Assert.assertEquals

class MainScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActionsAreVisible() {
        composeRule.onNodeWithText("PhotoCard Libre").assertIsDisplayed()
        composeRule.onNodeWithText("AGGIUNGI FOTO").assertIsDisplayed()
        composeRule.onNodeWithText("ANTEPRIMA").assertIsDisplayed()
        composeRule.onNodeWithText("CREA PDF").assertIsDisplayed()
        composeRule.onNodeWithTag("azioni_principali").assertIsDisplayed()
    }

    @Test
    fun applicationLabelUsesThePublicBrand() {
        val activity = composeRule.activity
        assertEquals("PhotoCard Libre", activity.getString(R.string.app_name))
        assertEquals(
            "PhotoCard Libre",
            activity.applicationInfo.loadLabel(activity.packageManager).toString(),
        )
    }

    @Test
    fun savedPdfActionsAreBothVisible() {
        composeRule.setContent {
            MaterialTheme {
                SavedPdfActions(onOpen = {}, onShare = {})
            }
        }

        composeRule.onNodeWithTag("azioni_pdf_salvato").assertIsDisplayed()
        composeRule.onNodeWithText("APRI PDF").assertIsDisplayed()
        composeRule.onNodeWithText("CONDIVIDI PDF").assertIsDisplayed()
    }

    @Test
    fun pdfNameDialogIsPrefilledAndAcceptsACustomName() {
        val defaultName = "PhotoCard_2026-09-19_1200.pdf"
        var savedName: String? = null
        composeRule.setContent {
            MaterialTheme {
                var name by remember { mutableStateOf(defaultName) }
                PdfNameDialog(
                    name = name,
                    error = null,
                    onNameChange = { name = it },
                    onCancel = {},
                    onSave = { savedName = name },
                )
            }
        }

        composeRule.onNodeWithText("NOME FILE PDF").assertIsDisplayed()
        composeRule.onNodeWithTag("nome_file_pdf").assertTextContains(defaultName)
        composeRule.onNodeWithTag("nome_file_pdf").performTextClearance()
        composeRule.onNodeWithTag("nome_file_pdf").performTextInput("VACANZE MARE")
        composeRule.onNodeWithText("SALVA").performClick()
        composeRule.runOnIdle { assertEquals("VACANZE MARE", savedName) }
    }
}
