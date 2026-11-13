package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shramsetu_prefs")

class SessionPreferences(private val context: Context) {
    private object Keys {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    val activeUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_USER_ID]
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[Keys.LANGUAGE_CODE])
    }

    suspend fun setActiveUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.ACTIVE_USER_ID)
            } else {
                prefs[Keys.ACTIVE_USER_ID] = userId
            }
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
        }
    }
}
