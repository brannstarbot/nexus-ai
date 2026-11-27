package com.nexus.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nexus.ai.navigation.NexusApp
import com.nexus.ai.ui.theme.NexusAITheme
import com.nexus.ai.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Tema dark sebagai default; pilihan pengguna akan dibaca dari Settings.
            NexusAITheme(themeMode = ThemeMode.DARK) {
                NexusApp(container = (application as NexusApplication).container)
            }
        }
    }
}
