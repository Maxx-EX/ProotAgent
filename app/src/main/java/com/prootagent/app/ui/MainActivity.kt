package com.prootagent.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prootagent.app.ui.screen.HomeScreen
import com.prootagent.app.ui.theme.ProotAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ProotAgentTheme {
                HomeScreen()
            }
        }
    }
}
