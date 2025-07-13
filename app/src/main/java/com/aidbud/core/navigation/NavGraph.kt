package com.aidbud.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

import com.aidbud.ui.pages.chat.ChatPage
import com.aidbud.ui.pages.loading.LoadingPage
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel

@Composable
fun AppNavHost(
    settingsViewModel: SettingsViewModel = viewModel(),
    cacheViewModel: GlobalCacheViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
    startDestination: String = "loading"
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("loading") {
            LoadingPage()
        }
        composable("camera") {
            CameraPage()
        }
    }
}