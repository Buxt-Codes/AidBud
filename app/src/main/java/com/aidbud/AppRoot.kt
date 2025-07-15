package com.aidbud

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController

import com.aidbud.core.navigation.AppNavHost
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.viewmodel.MainViewModel

@Composable
fun AppRoot() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val cacheViewModel: GlobalCacheViewModel = hiltViewModel()
    val aidBudViewModel: MainViewModel = hiltViewModel()

    val navController = rememberNavController()

    AppNavHost(
        navController = navController,
        settingsViewModel = settingsViewModel,
        cacheViewModel = cacheViewModel,
        aidBudViewModel = aidBudViewModel
    )
}