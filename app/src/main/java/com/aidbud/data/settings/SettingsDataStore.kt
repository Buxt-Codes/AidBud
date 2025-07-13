package com.aidbud.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "app_settings"

// Extension property on Context to create DataStore instance
val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

object SettingsDataStore {

    // Preference keys
    private val SAVE_IN_ALBUM = booleanPreferencesKey("save_in_album")
    private val SAVE_CONVERSATIONS = booleanPreferencesKey("save_conversations")
    private val CONVERSATIONS_LIMIT = intPreferencesKey("conversations_limit")

    // Save In Album
    fun getSaveInAlbum(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[SAVE_IN_ALBUM] ?: false
        }
    }

    suspend fun setSaveInAlbum(context: Context, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SAVE_IN_ALBUM] = value
        }
    }

    // Save Conversation
    fun getSaveConversation(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[SAVE_CONVERSATIONS] ?: true // default true
        }
    }

    suspend fun setSaveConversation(context: Context, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SAVE_CONVERSATIONS] = value
        }
    }

    // Conversation Limit
    fun getConversationLimit(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[CONVERSATIONS_LIMIT] ?: 100 // default limit
        }
    }

    suspend fun setConversationLimit(context: Context, limit: Int) {
        context.dataStore.edit { prefs ->
            prefs[CONVERSATIONS_LIMIT] = limit
        }
    }
}
