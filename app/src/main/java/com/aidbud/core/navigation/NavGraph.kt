package com.aidbud.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aidbud.ui.pages.chat.ChatPage
import com.aidbud.ui.pages.loading.LoadingPage

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "loading"
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("loading") {
            LoadingPage()
        }
        composable("chat") {
            ChatPage()
        }
    }
}