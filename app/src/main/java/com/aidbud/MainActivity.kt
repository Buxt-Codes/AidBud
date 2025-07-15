package com.aidbud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aidbud.ui.theme.AidBudTheme // Import your custom theme
import dagger.hilt.android.AndroidEntryPoint // Import for Hilt

/**
 * The main Activity of the AidBud application.
 * This is the primary entry point for the user interface.
 *
 * Annotated with @AndroidEntryPoint to enable Hilt for dependency injection within
 * this Activity and its hosted Composables (like ViewModels obtained via hiltViewModel()).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the Compose content for this Activity
        setContent {
            // Apply your custom application theme
            AidBudTheme {
                // A Surface container using the 'background' color from the theme.
                // This provides a default background for your entire app content.
                Surface(
                    modifier = Modifier.fillMaxSize(), // Make the surface fill the entire screen
                    color = MaterialTheme.colorScheme.background // Use the background color from your theme
                ) {
                    // This is where your application's root Composable is called.
                    // AppRoot will handle setting up navigation and providing ViewModels.
                    AppRoot()
                }
            }
        }
    }
}