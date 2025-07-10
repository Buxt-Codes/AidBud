package com.aidbud

import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import android.os.Bundle
import com.aidbud.core.navigation.AppNavHost
import com.aidbud.ui.theme.AidBudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AidBudTheme {
                AppNavHost()
            }
        }
    }
}