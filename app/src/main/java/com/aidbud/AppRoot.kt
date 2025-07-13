package com.aidbud

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

import com.aidbud.core.navigation.AppNavHost
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel

@Composable
fun AppRoot() {
    val settingsViewModel: SettingsViewModel = viewModel()
    val cacheViewModel: GlobalCacheViewModel = viewModel()

    AppNavHost(
        settingsViewModel = settingsViewModel,
        cacheViewModel = cacheViewModel
    )
}