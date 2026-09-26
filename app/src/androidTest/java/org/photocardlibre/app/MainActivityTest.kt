package org.photocardlibre.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.photocardlibre.app.settings.AppLanguage
import org.photocardlibre.app.settings.SettingsRepository

class MainActivityTest {
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
    fun applicationLabelUsesThePublicBrand() {
        val activity = composeRule.activity
        assertEquals("PhotoCard Libre", activity.getString(R.string.app_name))
        assertEquals(
            "PhotoCard Libre",
            activity.applicationInfo.loadLabel(activity.packageManager).toString(),
        )
    }
}
