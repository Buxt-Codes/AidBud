package com.aidbud.data.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ViewModel for managing application settings.
 * It injects SettingsDataStore to interact with persistent settings.
 */
@Singleton
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore // Inject the DataStore class
) : ViewModel() {


    val conversationLimit: StateFlow<Int> = settingsDataStore.conversationLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)


    fun setConversationLimit(limit: Int) {
        viewModelScope.launch {
            settingsDataStore.setConversationLimit(limit)
        }
    }
}