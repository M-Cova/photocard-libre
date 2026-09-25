package org.photocardlibre.app

import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.IntentCompat
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.photocardlibre.app.export.PdfExport

class CacheLifecycleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun configurationChangeDoesNotDeleteCurrentFileProviderPdf() {
        val output = File(composeRule.activity.cacheDir, "photo_output")
        assertTrue(output.mkdirs() || output.isDirectory)
        val pdf = File(output, "foto-album.pdf").apply {
            writeBytes("%PDF-current".toByteArray())
        }
        val shareIntent = PdfExport.createShareIntent(composeRule.activity, pdf.absolutePath)
        val sharedUri = IntentCompat.getParcelableExtra(
            shareIntent,
            Intent.EXTRA_STREAM,
            android.net.Uri::class.java,
        )

        composeRule.activityRule.scenario.recreate()

        assertTrue(pdf.exists())
        assertEquals("content", sharedUri?.scheme)
        assertTrue(shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
