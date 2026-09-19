package org.photocardlibre.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import org.photocardlibre.app.ui.AlbumScreen
import org.photocardlibre.app.ui.theme.PhotoCardLibreTheme

class MainActivity : ComponentActivity() {
    private val albumViewModel: AlbumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoCardLibreTheme {
                AlbumScreen(albumViewModel)
            }
        }
    }
}
