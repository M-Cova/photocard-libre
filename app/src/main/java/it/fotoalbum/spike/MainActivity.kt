package it.fotoalbum.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import it.fotoalbum.spike.ui.AlbumScreen

class MainActivity : ComponentActivity() {
    private val albumViewModel: AlbumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AlbumScreen(albumViewModel)
            }
        }
    }
}
