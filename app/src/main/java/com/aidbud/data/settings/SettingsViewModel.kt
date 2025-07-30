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

    val triageEnabled: StateFlow<Boolean> = settingsDataStore.triageEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val firstAidEnabled: StateFlow<Boolean> = settingsDataStore.firstAidEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val contextEnabled: StateFlow<Boolean> = settingsDataStore.contextEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val triage: StateFlow<Map<String, String>> = settingsDataStore.triage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val firstAidAccess: StateFlow<FirstAidAccess> = settingsDataStore.firstAidAccess
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FirstAidAccess.NON_IMMEDIATE)

    val currentContext: StateFlow<Pair<CurrentContext, String?>> = settingsDataStore.currentContext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(CurrentContext.DAY_TO_DAY, null))


    fun setConversationLimit(limit: Int) {
        viewModelScope.launch {
            settingsDataStore.setConversationLimit(limit)
        }
    }

    fun setTriageEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTriageEnabled(enabled)
        }
    }

    fun setFirstAidEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setFirstAidEnabled(enabled)
        }
    }

    fun setContextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setContextEnabled(enabled)
        }
    }

    fun setTriage(triageMap: Map<String, String>) {
        viewModelScope.launch {
            settingsDataStore.setTriage(triageMap)
        }
    }

    fun setFirstAidAccess(access: FirstAidAccess) {
        viewModelScope.launch {
            settingsDataStore.setFirstAidAccess(access)
        }
    }

    fun setCurrentContext(context: CurrentContext, customValue: String? = null) {
        viewModelScope.launch {
            settingsDataStore.setCurrentContext(context, customValue)
        }
    }
}