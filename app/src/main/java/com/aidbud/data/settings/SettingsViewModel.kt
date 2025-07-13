package com.aidbud.data.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Expose settings as StateFlow
    val saveInAlbum = SettingsDataStore.getSaveInAlbum(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val saveConversation = SettingsDataStore.getSaveConversation(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val conversationLimit = SettingsDataStore.getConversationLimit(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    // Update functions
    fun setSaveInAlbum(enabled: Boolean) {
        viewModelScope.launch {
            SettingsDataStore.setSaveInAlbum(context, enabled)
        }
    }

    fun setSaveConversation(enabled: Boolean) {
        viewModelScope.launch {
            SettingsDataStore.setSaveConversation(context, enabled)
        }
    }

    fun setConversationLimit(limit: Int) {
        viewModelScope.launch {
            SettingsDataStore.setConversationLimit(context, limit)
        }
    }
}
