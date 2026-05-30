package net.aquadx.aquacard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import net.aquadx.aquacard.nfc.HceController
import net.aquadx.aquacard.ui.screens.MainAppScreen
import net.aquadx.aquacard.ui.theme.AquaCardTheme

class MainActivity : ComponentActivity() {

    private lateinit var hceController: HceController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        hceController = HceController(this)

        setContent {
            AquaCardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(
                        hceController = hceController,
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}
