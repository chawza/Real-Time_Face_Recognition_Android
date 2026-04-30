package com.atharvakale.facerecognition.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val DISTANCE_THRESHOLD = floatPreferencesKey("distance_threshold")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
    }

    val distanceThreshold: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[DISTANCE_THRESHOLD] ?: 0.3f
    }

    val developerMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEVELOPER_MODE] ?: false
    }

    suspend fun updateDistanceThreshold(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[DISTANCE_THRESHOLD] = value
        }
    }

    suspend fun toggleDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DEVELOPER_MODE] = enabled
        }
    }
}
