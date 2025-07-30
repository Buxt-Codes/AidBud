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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONObject

// Define the DataStore instance as an extension property on Context
// This should be a top-level property in a file accessible by your DataStore class.
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages application settings using Jetpack DataStore.
 * This class is a singleton and its dependencies are injected by Hilt.
 */

enum class FirstAidAccess {
    IMMEDIATE,
    NON_IMMEDIATE,
    NO_ACCESS
}

// Enum for Current Context
enum class CurrentContext {
    CRISIS_EARTHQUAKE,
    TSUNAMI,
    LANDSLIDE,
    DAY_TO_DAY,
    CUSTOM // For custom context, the value will be stored as a string
}

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val CONVERSATION_LIMIT_KEY = intPreferencesKey("conversation_limit")
    private val TRIAGE_ENABLED_KEY = booleanPreferencesKey("triage_enabled")
    private val FIRST_AID_ENABLED_KEY = booleanPreferencesKey("first_aid_enabled")
    private val CONTEXT_ENABLED_KEY = booleanPreferencesKey("context_enabled")
    private val TRIAGE_KEY = stringPreferencesKey("triage")
    private val FIRST_AID_ACCESS_KEY = stringPreferencesKey("first_aid_access")
    private val CURRENT_CONTEXT_KEY = stringPreferencesKey("current_context")
    private val CUSTOM_CONTEXT_VALUE_KEY = stringPreferencesKey("custom_context_value")

    val conversationLimit: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[CONVERSATION_LIMIT_KEY] ?: 100
    }

    val triageEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[TRIAGE_ENABLED_KEY] ?: false }
    val firstAidEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[FIRST_AID_ENABLED_KEY] ?: false }
    val contextEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[CONTEXT_ENABLED_KEY] ?: false }

    val triage: Flow<Map<String, String>> = context.settingsDataStore.data.map { preferences ->
        val jsonString = preferences[TRIAGE_KEY] ?: "{}"
        parseJsonToMap(jsonString)
    }

    val firstAidAccess: Flow<FirstAidAccess> = context.settingsDataStore.data.map { preferences ->
        val accessString = preferences[FIRST_AID_ACCESS_KEY] ?: FirstAidAccess.NON_IMMEDIATE.name
        FirstAidAccess.valueOf(accessString)
    }

    val currentContext: Flow<Pair<CurrentContext, String?>> = context.settingsDataStore.data.map { preferences ->
        val contextString = preferences[CURRENT_CONTEXT_KEY] ?: CurrentContext.DAY_TO_DAY.name
        val customValue = preferences[CUSTOM_CONTEXT_VALUE_KEY]
        val contextEnum = CurrentContext.valueOf(contextString)
        Pair(contextEnum, if (contextEnum == CurrentContext.CUSTOM) customValue else null)
    }

    suspend fun setConversationLimit(limit: Int) {
        context.settingsDataStore.edit { settings ->
            settings[CONVERSATION_LIMIT_KEY] = limit
        }
    }

    suspend fun setTriageEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[TRIAGE_ENABLED_KEY] = enabled }
    }

    suspend fun setFirstAidEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[FIRST_AID_ENABLED_KEY] = enabled }
    }

    suspend fun setContextEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[CONTEXT_ENABLED_KEY] = enabled }
    }

    suspend fun setTriage(triageMap: Map<String, String>) {
        context.settingsDataStore.edit { settings ->
            settings[TRIAGE_KEY] = mapToJsonString(triageMap)
        }
    }

    suspend fun setFirstAidAccess(access: FirstAidAccess) {
        context.settingsDataStore.edit { settings ->
            settings[FIRST_AID_ACCESS_KEY] = access.name
        }
    }

    suspend fun setCurrentContext(currentContext: CurrentContext, customValue: String? = null) {
        context.settingsDataStore.edit { settings ->
            settings[CURRENT_CONTEXT_KEY] = currentContext.name
            if (currentContext == CurrentContext.CUSTOM) {
                settings[CUSTOM_CONTEXT_VALUE_KEY] = customValue ?: ""
            } else {
                settings.remove(CUSTOM_CONTEXT_VALUE_KEY)
            }
        }
    }

    // Helper function to parse JSON string to Map
    private fun parseJsonToMap(jsonString: String): Map<String, String> {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().asSequence().associateWith { key -> jsonObject.getString(key) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun mapToJsonString(map: Map<String, String>): String {
        return JSONObject(map).toString()
    }
}