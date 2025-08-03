package com.aidbud.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.ui.pages.camera.CameraPage
import com.aidbud.data.viewmodel.MainViewModel // Corrected import for AidBudViewModel
import com.aidbud.ui.pages.chat.ChatPage
import com.aidbud.ui.pages.loading.LoadingPage
import com.aidbud.ui.pages.pcard.PCardPage
import kotlinx.coroutines.launch

/**
 * Defines the navigation graph for the entire application.
 * It receives shared ViewModels and passes them to the specific screens.
 *
 * @param navController The NavHostController for managing navigation state.
 * @param settingsViewModel The ViewModel for application settings.
 * @param cacheViewModel The ViewModel for global caching (e.g., draft messages).
 * @param aidBudViewModel The comprehensive ViewModel for conversations, messages, and LLM.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel,
    aidBudViewModel: MainViewModel // Use AidBudViewModel as per previous discussion
) {
    val coroutineScope = rememberCoroutineScope()

    // Set "initial_setup" as the starting destination
    NavHost(navController = navController, startDestination = "loading_page") {

        composable("loading_page") {
            LoadingPage(
                navController = navController,
                viewModel = aidBudViewModel
            )
        }

        composable("initial_setup") {
            // Show a loading spinner or splash screen here while the conversation is being created
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Initializing app...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray
                    )
                )
            }
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val newConversationId = aidBudViewModel.insertConversation("New Conversation")
                    navController.navigate("camera_page/$newConversationId") {
                        popUpTo("initial_setup") { inclusive = true }
                    }
                }
            }
        }

        composable(
            "camera_page/{conversationId}", // Define argument in the route
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            // Extract the conversationId from the navigation arguments
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: -1L
            cacheViewModel.setCurrentConversationId(conversationId)
            // Pass all necessary ViewModels and the conversationId to the CameraPage
            CameraPage(
                conversationId = conversationId, // Pass the ID
                navController = navController,
                viewModel = aidBudViewModel,
                settingsViewModel = settingsViewModel,
                cacheViewModel = cacheViewModel
            )
        }

        composable(
            "chat_page/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: -1L
            cacheViewModel.setCurrentConversationId(conversationId)
            // Call the ChatPage composable with all required dependencies
            ChatPage(
                conversationId = conversationId,
                navController = navController,
                viewModel = aidBudViewModel,
                settingsViewModel = settingsViewModel,
                cacheViewModel = cacheViewModel
            )
        }

        composable(
            "pcard_page/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: -1L
            cacheViewModel.setCurrentConversationId(conversationId)
            // Call the PCardPage composable with all required dependencies
            PCardPage(
                conversationId = conversationId,
                navController = navController,
                viewModel = aidBudViewModel,
                settingsViewModel = settingsViewModel,
                cacheViewModel = cacheViewModel
            )
        }

    }
}