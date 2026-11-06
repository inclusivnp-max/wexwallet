package wex.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import wex.wallet.core.designsystem.theme.WexTheme
import wex.wallet.ui.WexApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WexTheme {
                WexApp()
            }
        }
    }
}
