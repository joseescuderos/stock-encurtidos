package com.photosdbrowser.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.photosdbrowser.app.data.AppSettings
import com.photosdbrowser.app.data.SettingsRepository
import com.photosdbrowser.app.navigation.PhotoBrowserNavGraph
import com.photosdbrowser.app.ui.theme.PhotoSDBrowserTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val settings by remember { settingsRepository.settings }
                .collectAsState(initial = AppSettings())

            PhotoSDBrowserTheme(darkTheme = settings.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoBrowserNavGraph()
                }
            }
        }
    }
}
