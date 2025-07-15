package com.aidbud.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Define the DataStore instance as an extension property on Context
// This should be a top-level property in a file accessible by your DataStore class.
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages application settings using Jetpack DataStore.
 * This class is a singleton and its dependencies are injected by Hilt.
 */
@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val SAVE_IN_ALBUM_KEY = booleanPreferencesKey("save_in_album")
    private val SAVE_CONVERSATION_KEY = booleanPreferencesKey("save_conversation")
    private val CONVERSATION_LIMIT_KEY = intPreferencesKey("conversation_limit")

    val saveInAlbum: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SAVE_IN_ALBUM_KEY] ?: false
    }

    val saveConversation: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SAVE_CONVERSATION_KEY] ?: true
    }

    val conversationLimit: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[CONVERSATION_LIMIT_KEY] ?: 100
    }

    suspend fun setSaveInAlbum(enabled: Boolean) {
        context.settingsDataStore.edit { settings ->
            settings[SAVE_IN_ALBUM_KEY] = enabled
        }
    }

    suspend fun setSaveConversation(enabled: Boolean) {
        context.settingsDataStore.edit { settings ->
            settings[SAVE_CONVERSATION_KEY] = enabled
        }
    }

    suspend fun setConversationLimit(limit: Int) {
        context.settingsDataStore.edit { settings ->
            settings[CONVERSATION_LIMIT_KEY] = limit
        }
    }
}