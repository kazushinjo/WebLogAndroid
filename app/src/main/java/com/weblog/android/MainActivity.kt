package com.weblog.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.weblog.android.ui.WebLogApp
import com.weblog.android.utils.JCCStore

private val HighContrastLightColors = lightColorScheme(
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    onPrimaryContainer = Color.Black,
    onSecondaryContainer = Color.Black,
    onTertiaryContainer = Color.Black,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JCCStore.init(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = HighContrastLightColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WebLogApp()
                }
            }
        }
    }
}
