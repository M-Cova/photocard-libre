package org.photocardlibre.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.junit.Before
import org.junit.Test
import org.photocardlibre.app.ui.SavedPdfActions
import org.photocardlibre.app.ui.PdfNameDialog
import org.photocardlibre.app.ui.AlbumGallery
import org.photocardlibre.app.ui.SelectedPhotoEditor
import org.photocardlibre.app.model.PhotoEntry
import org.junit.Assert.assertEquals
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

class MainScreenTest {
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
    fun emptyAlbumHasOneClearStartingAction() {
        composeRule.onNodeWithText("PhotoCard Libre").assertIsDisplayed()
        composeRule.onNodeWithText("Your album starts here").assertIsDisplayed()
        composeRule.onNodeWithText("ADD PHOTOS").assertIsDisplayed()
        composeRule.onNodeWithText("PREVIEW PDF").assertDoesNotExist()
        composeRule.onNodeWithText("CREATE PDF").assertDoesNotExist()
        composeRule.onNodeWithTag("album_vuoto").assertIsDisplayed()
    }

    @Test
    fun populatedAlbumExplainsPhotoEditingAndNamesPdfPreview() {
        var selectedId: String? = null
        composeRule.setContent {
            MaterialTheme {
                AlbumGallery(
                    photos = listOf(PhotoEntry("first", "/missing-photo.jpg", "First")),
                    message = null,
                    onAdd = {},
                    onSettings = {},
                    onPhoto = { selectedId = it },
                    onMove = { _, _ -> },
                    onPreview = {},
                )
            }
        }

        composeRule.onNodeWithText("Tap a photo to edit it.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selected photo").assertDoesNotExist()
        composeRule.onNodeWithText("PREVIEW PDF").assertIsDisplayed()
        composeRule.onNodeWithTag("foto_first").performClick()
        composeRule.runOnIdle { assertEquals("first", selectedId) }
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
        composeRule.onNodeWithText("OPEN PDF").assertIsDisplayed()
        composeRule.onNodeWithText("SHARE PDF").assertIsDisplayed()
    }

    @Test
    fun albumReorderButtonsMoveTheirOwnPhotoWithoutOpeningEditor() {
        var photos by mutableStateOf(listOf(
            PhotoEntry("first", "/missing-photo.jpg", "First"),
            PhotoEntry("second", "/missing-photo.jpg", "Second"),
        ))
        var openedPhoto: String? = null
        composeRule.setContent {
            MaterialTheme {
                AlbumGallery(
                    photos = photos,
                    message = null,
                    onAdd = {},
                    onSettings = {},
                    onPhoto = { openedPhoto = it },
                    onMove = { id, offset ->
                        photos = org.photocardlibre.app.model.AlbumState(photos)
                            .move(id, offset).photos
                    },
                    onPreview = {},
                )
            }
        }

        composeRule.onNodeWithText("Order in album").assertIsDisplayed()
        composeRule.onNodeWithTag("sposta_first_prima").assertIsNotEnabled()
        composeRule.onNodeWithTag("sposta_first_dopo").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("second", "first"), photos.map { it.id })
            assertEquals(null, openedPhoto)
        }
    }

    @Test
    fun deletingPhotoRequiresExplicitConfirmation() {
        var deleteCount = 0
        composeRule.setContent {
            MaterialTheme {
                SelectedPhotoEditor(
                    photo = PhotoEntry("first", "/missing-photo.jpg", "First.jpg"),
                    onCaptionChange = {},
                    onDelete = { deleteCount++ },
                    onEdit = {},
                )
            }
        }

        composeRule.onNodeWithTag("elimina_foto").performClick()
        composeRule.onNodeWithText("Delete this photo?").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, deleteCount) }
        composeRule.onNodeWithTag("annulla_elimina_foto").performClick()
        composeRule.runOnIdle { assertEquals(0, deleteCount) }

        composeRule.onNodeWithTag("elimina_foto").performClick()
        composeRule.onNodeWithTag("conferma_elimina_foto").performClick()
        composeRule.runOnIdle { assertEquals(1, deleteCount) }
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

        composeRule.onNodeWithText("PDF FILE NAME").assertIsDisplayed()
        composeRule.onNodeWithTag("nome_file_pdf").assertTextContains(defaultName)
        composeRule.onNodeWithTag("nome_file_pdf").performTextClearance()
        composeRule.onNodeWithTag("nome_file_pdf").performTextInput("VACANZE MARE")
        composeRule.onNodeWithText("SAVE").performClick()
        composeRule.runOnIdle { assertEquals("VACANZE MARE", savedName) }
    }
}
