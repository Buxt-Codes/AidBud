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

/**
 * ViewModel for managing application settings.
 * It injects SettingsDataStore to interact with persistent settings.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore // Inject the DataStore class
) : ViewModel() {

    // Expose settings as StateFlows, directly from the DataStore
    val saveInAlbum: StateFlow<Boolean> = settingsDataStore.saveInAlbum
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val saveConversation: StateFlow<Boolean> = settingsDataStore.saveConversation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val conversationLimit: StateFlow<Int> = settingsDataStore.conversationLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    // Update functions
    fun setSaveInAlbum(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSaveInAlbum(enabled)
        }
    }

    fun setSaveConversation(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSaveConversation(enabled)
        }
    }

    fun setConversationLimit(limit: Int) {
        viewModelScope.launch {
            settingsDataStore.setConversationLimit(limit)
        }
    }
}