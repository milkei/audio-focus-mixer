package dev.milkei.audiofocusmixer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dev.milkei.audiofocusmixer.ui.MixerScreen
import dev.milkei.audiofocusmixer.ui.theme.AudioFocusMixerTheme

/** No custom volume panel, overlays, or accessibility services. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioFocusMixerTheme {
                Surface {
                    MixerScreen()
                }
            }
        }
    }
}
