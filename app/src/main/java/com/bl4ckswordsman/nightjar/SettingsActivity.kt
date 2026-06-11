package com.bl4ckswordsman.nightjar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.bl4ckswordsman.nightjar.ui.screen.SettingsScreen
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightjarTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
