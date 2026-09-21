package org.photocardlibre.app

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.photocardlibre.app.model.CropRect
import org.photocardlibre.app.model.PhotoEntry
import org.photocardlibre.app.ui.CropEditorScreen
import org.photocardlibre.app.ui.SelectedPhotoEditor
import org.photocardlibre.app.ui.cropBitmap
import org.photocardlibre.app.ui.decodeOrientedBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import java.io.File

class CropEditorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun useItalian() {
        runBlocking {
            SettingsRepository(composeRule.activity).setAppLanguage(AppLanguage.ITALIAN)
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun editPhotoOpensAFullScreenDestinationWithoutDialog() {
        val photo = testPhoto()
        composeRule.setContent {
            MaterialTheme {
                var editorVisible by remember { mutableStateOf(false) }
                if (editorVisible) {
                    CropEditorScreen(photo, onBack = { editorVisible = false }, onConfirm = {})
                } else {
                    SelectedPhotoEditor(
                        photo = photo,
                        onCaptionChange = {},
                        onMoveBefore = {},
                        onMoveAfter = {},
                        onDelete = {},
                        onEdit = { editorVisible = true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("MODIFICA FOTO").performClick()
        composeRule.onNodeWithTag("editor_crop").assertIsDisplayed()
        composeRule.onNodeWithText("CONFERMA").assertIsDisplayed().assertHasClickAction()
        assertTrue(composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun editorShowsOnlyRequiredPresetsAndConfirm() {
        setEditor()

        listOf("ORIGINALE", "1:1", "4:3", "3:4").forEach { label ->
            composeRule.onNodeWithText(label).fetchSemanticsNode()
        }
        composeRule.onNodeWithText("CONFERMA").assertIsDisplayed().assertHasClickAction()
        assertTrue(composeRule.onAllNodesWithText("3:2").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("RIPRISTINA").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("ANNULLA").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun confirmAppliesCropUpdatesPreviewAndReturnsToMain() {
        val photo = testPhoto()
        var confirmed: CropRect? = null
        composeRule.setContent {
            MaterialTheme {
                var editorVisible by remember { mutableStateOf(true) }
                if (editorVisible) {
                    CropEditorScreen(
                        photo = photo,
                        onBack = { editorVisible = false },
                        onConfirm = {
                            confirmed = it
                            editorVisible = false
                        },
                    )
                } else {
                    Text("SCHERMATA PRINCIPALE")
                }
            }
        }

        composeRule.onNodeWithText("1:1").performClick()
        composeRule.onNodeWithText("CONFERMA").performClick()
        composeRule.runOnIdle {
            val crop = requireNotNull(confirmed)
            val bitmap = requireNotNull(decodeOrientedBitmap(photo.localPath))
            val preview = cropBitmap(bitmap, crop)
            assertEquals(preview.width, preview.height)
        }
        composeRule.onNodeWithText("SCHERMATA PRINCIPALE").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("editor_crop").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun originalThenConfirmRemovesPreviousCropAndReturnsToMain() {
        val previous = CropRect(0.125, 0.0, 0.75, 1.0)
        val photo = testPhoto(previous)
        var confirmed: CropRect? = previous
        composeRule.setContent {
            MaterialTheme {
                var editorVisible by remember { mutableStateOf(true) }
                if (editorVisible) {
                    CropEditorScreen(
                        photo = photo,
                        onBack = { editorVisible = false },
                        onConfirm = {
                            confirmed = it
                            editorVisible = false
                        },
                    )
                } else {
                    Text("SCHERMATA PRINCIPALE")
                }
            }
        }

        composeRule.onNodeWithText("ORIGINALE").performClick()
        composeRule.onNodeWithText("CONFERMA").performClick()
        composeRule.runOnIdle { assertNull(confirmed) }
        composeRule.onNodeWithText("SCHERMATA PRINCIPALE").assertIsDisplayed()
    }

    @Test
    fun systemBackDoesNotSaveTemporaryCrop() {
        val previous = CropRect(0.125, 0.0, 0.75, 1.0)
        val photo = testPhoto(previous)
        var savedCrop: CropRect? = previous
        composeRule.setContent {
            MaterialTheme {
                var editorVisible by remember { mutableStateOf(true) }
                if (editorVisible) {
                    CropEditorScreen(
                        photo = photo,
                        onBack = { editorVisible = false },
                        onConfirm = {
                            savedCrop = it
                            editorVisible = false
                        },
                    )
                } else {
                    Text("SCHERMATA PRINCIPALE")
                }
            }
        }

        composeRule.onNodeWithText("1:1").performClick()
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.runOnIdle { assertEquals(previous, savedCrop) }
        composeRule.onNodeWithText("SCHERMATA PRINCIPALE").assertIsDisplayed()
    }

    @Test
    fun topBackDoesNotSaveTemporaryCrop() {
        val previous = CropRect(0.125, 0.0, 0.75, 1.0)
        val photo = testPhoto(previous)
        var savedCrop: CropRect? = previous
        composeRule.setContent {
            MaterialTheme {
                var editorVisible by remember { mutableStateOf(true) }
                if (editorVisible) {
                    CropEditorScreen(
                        photo = photo,
                        onBack = { editorVisible = false },
                        onConfirm = { savedCrop = it },
                    )
                } else {
                    Text("SCHERMATA PRINCIPALE")
                }
            }
        }

        composeRule.onNodeWithText("3:4").performClick()
        composeRule.onNodeWithTag("indietro_crop").performClick()
        composeRule.runOnIdle { assertEquals(previous, savedCrop) }
        composeRule.onNodeWithText("SCHERMATA PRINCIPALE").assertIsDisplayed()
    }

    @Test
    fun confirmIsVisibleAt320By400Portrait() {
        setPortraitEditor(width = 320, height = 400)
        assertConfirmVisibleAndClickable()
    }

    @Test
    fun confirmIsVisibleAt360By640Portrait() {
        setPortraitEditor(width = 360, height = 640)
        assertConfirmVisibleAndClickable()
    }

    @Test
    fun confirmIsVisibleAt411By891Portrait() {
        setPortraitEditor(width = 411, height = 891)
        assertConfirmVisibleAndClickable()
    }

    @Test
    fun simulatedNavigationBarStaysBelowConfirm() {
        val photo = testPhoto()
        composeRule.setContent {
            MaterialTheme {
                val bottomInset = with(LocalDensity.current) { 48.dp.roundToPx() }
                Box(
                    modifier = Modifier
                        .requiredSize(320.dp, 400.dp)
                        .clipToBounds()
                        .testTag("viewport_portrait"),
                ) {
                    CropEditorScreen(
                        photo = photo,
                        onBack = {},
                        onConfirm = {},
                        navigationBarInsets = WindowInsets(0, 0, 0, bottomInset),
                    )
                }
            }
        }

        assertConfirmVisibleAndClickable()
        val viewportBottom = composeRule.onNodeWithTag("viewport_portrait")
            .getUnclippedBoundsInRoot().bottom
        val confirmBottom = composeRule.onNodeWithTag("conferma_crop")
            .getUnclippedBoundsInRoot().bottom
        assertTrue(confirmBottom <= viewportBottom - 48.dp)
    }

    private fun setEditor() {
        val photo = testPhoto()
        composeRule.setContent {
            MaterialTheme {
                CropEditorScreen(photo = photo, onBack = {}, onConfirm = {})
            }
        }
    }

    private fun setPortraitEditor(width: Int, height: Int) {
        val photo = testPhoto()
        composeRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .requiredSize(width.dp, height.dp)
                        .clipToBounds(),
                ) {
                    CropEditorScreen(
                        photo = photo,
                        onBack = {},
                        onConfirm = {},
                        navigationBarInsets = WindowInsets(0, 0, 0, 0),
                    )
                }
            }
        }
    }

    private fun assertConfirmVisibleAndClickable() {
        composeRule.onNodeWithText("CONFERMA")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    private fun testPhoto(crop: CropRect? = null): PhotoEntry {
        val file = File(composeRule.activity.cacheDir, "crop-editor-test.png")
        Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).run {
            eraseColor(android.graphics.Color.BLUE)
            file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        return PhotoEntry("crop-test", file.absolutePath, file.name, crop = crop)
    }
}
