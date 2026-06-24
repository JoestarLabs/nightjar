package com.bl4ckswordsman.nightjar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.bl4ckswordsman.nightjar.ui.screen.DependenciesScreen
import com.bl4ckswordsman.nightjar.ui.theme.NightjarTheme

class DependenciesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightjarTheme {
                DependenciesScreen(
                    onNavigateBack = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
