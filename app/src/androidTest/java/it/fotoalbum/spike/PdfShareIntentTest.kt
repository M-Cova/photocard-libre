package it.fotoalbum.spike

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.IntentCompat
import it.fotoalbum.spike.export.PdfExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfShareIntentTest {
    @Test
    fun openAndShareUseTheRenamedPdfContentUri() {
        val savedUri = Uri.parse("content://media/external/downloads/VACANZE%20MARE.pdf")
        val openIntent = PdfExport.createOpenIntent(savedUri)
        val shareIntent = PdfExport.createShareIntent(savedUri)
        val sharedUri = IntentCompat.getParcelableExtra(
            shareIntent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )

        assertEquals(Intent.ACTION_VIEW, openIntent.action)
        assertEquals("application/pdf", openIntent.type)
        assertEquals(savedUri, openIntent.data)
        assertTrue(openIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(Intent.ACTION_SEND, shareIntent.action)
        assertEquals("application/pdf", shareIntent.type)
        assertEquals(savedUri, sharedUri)
        assertTrue(shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun shareUsesFileProviderContentUriAndReadGrant() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(context.cacheDir, "photo_output").apply { mkdirs() }
        val pdf = File(output, "condivisione-test.pdf").apply { writeBytes("%PDF-test".toByteArray()) }
        try {
            val intent = PdfExport.createShareIntent(context, pdf.absolutePath)
            val uri = IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_STREAM,
                android.net.Uri::class.java,
            )
            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("application/pdf", intent.type)
            assertEquals("content", uri?.scheme)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        } finally {
            pdf.delete()
        }
    }
}
