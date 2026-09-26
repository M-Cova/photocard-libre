package org.photocardlibre.app

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
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
import org.junit.Test
import org.photocardlibre.app.ui.SavedPdfActions
import org.photocardlibre.app.ui.PdfNameDialog
import org.photocardlibre.app.ui.AlbumGallery
import org.photocardlibre.app.ui.SelectedPhotoEditor
import org.photocardlibre.app.model.PhotoEntry
import org.junit.Assert.assertEquals

class MainScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

        composeRule.onNodeWithText(string(R.string.album_tap_to_edit)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.selected_photo_description)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.action_preview)).assertIsDisplayed()
        composeRule.onNodeWithTag("foto_first").performClick()
        composeRule.runOnIdle { assertEquals("first", selectedId) }
    }

    @Test
    fun savedPdfActionsAreBothVisible() {
        composeRule.setContent {
            MaterialTheme {
                SavedPdfActions(onOpen = {}, onShare = {})
            }
        }

        composeRule.onNodeWithTag("azioni_pdf_salvato").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.open_pdf)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.share_pdf)).assertIsDisplayed()
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

        composeRule.onNodeWithText(string(R.string.reorder_heading)).assertIsDisplayed()
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
        composeRule.onNodeWithText(string(R.string.delete_photo_confirmation_title)).assertIsDisplayed()
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

        composeRule.onNodeWithText(string(R.string.pdf_name_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("nome_file_pdf").assertTextContains(defaultName)
        composeRule.onNodeWithTag("nome_file_pdf").performTextClearance()
        composeRule.onNodeWithTag("nome_file_pdf").performTextInput("VACANZE MARE")
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.runOnIdle { assertEquals("VACANZE MARE", savedName) }
    }

    private fun string(@StringRes resource: Int): String = composeRule.activity.getString(resource)
}
