package com.aidbud.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.ui.pages.camera.CameraPage
import com.aidbud.data.viewmodel.MainViewModel // Corrected import for AidBudViewModel
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
    NavHost(navController = navController, startDestination = "initial_setup") {

        // This composable handles the initial setup logic (creating a new conversation)
        composable("initial_setup") {
            LaunchedEffect(Unit) { // This effect runs only once when the composable enters composition
                coroutineScope.launch {
                    // Create a new conversation and get its ID
                    val newConversationId = aidBudViewModel.insertConversation("New Conversation")

                    // Navigate to the camera_screen, passing the new conversation ID
                    // popUpTo ensures that 'initial_setup' is removed from the back stack,
                    // so pressing back from CameraPage won't go back to this setup screen.
                    navController.navigate("camera_screen/$newConversationId") {
                        popUpTo("initial_setup") { inclusive = true }
                    }
                }
            }
            // You can show a loading spinner or splash screen here while the conversation is being created
            // Text("Initializing app...")
        }

        composable(
            "camera_screen/{conversationId}", // Define argument in the route
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
        // If you want to add other screens later, you'd add more composable blocks here:
        // composable("conversation_list") {
        //     ConversationListScreen(
        //         navController = navController,
        //         viewModel = aidBudViewModel,
        //         settingsViewModel = settingsViewModel,
        //         cacheViewModel = cacheViewModel
        //     )
        // }
        // composable(
        //     "chat_screen/{conversationId}",
        //     arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        // ) { backStackEntry ->
        //     val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: -1L
        //     ChatScreen(
        //         conversationId = conversationId,
        //         navController = navController,
        //         viewModel = aidBudViewModel,
        //         cacheViewModel = cacheViewModel
        //     )
        // }
    }
}