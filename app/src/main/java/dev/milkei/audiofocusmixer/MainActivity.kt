package dev.milkei.audiofocusmixer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dev.milkei.audiofocusmixer.ui.MixerScreen

/** Normal activity only: does not intercept hardware volume buttons or draw overlays. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    MixerScreen()
                }
            }
        }
    }
}
