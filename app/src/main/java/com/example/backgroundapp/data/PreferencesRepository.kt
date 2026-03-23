package com.example.backgroundapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "monitor_settings")

class PreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val settings: Flow<MonitorSettings> = dataStore.data.map { prefs ->
        MonitorSettings(
            destinationEmail = prefs[KEY_EMAIL].orEmpty(),
            uploadEndpoint = prefs[KEY_UPLOAD_ENDPOINT].orEmpty(),
            monitoringActive = prefs[KEY_MONITORING_ACTIVE] ?: false,
        )
    }

    suspend fun setDestinationEmail(value: String) {
        dataStore.edit { it[KEY_EMAIL] = value.trim() }
    }

    suspend fun setUploadEndpoint(value: String) {
        dataStore.edit { it[KEY_UPLOAD_ENDPOINT] = value.trim() }
    }

    suspend fun setMonitoringActive(active: Boolean) {
        dataStore.edit { it[KEY_MONITORING_ACTIVE] = active }
    }

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("destination_email")
        private val KEY_UPLOAD_ENDPOINT = stringPreferencesKey("upload_endpoint")
        private val KEY_MONITORING_ACTIVE = booleanPreferencesKey("monitoring_active")
    }
}

data class MonitorSettings(
    val destinationEmail: String,
    val uploadEndpoint: String,
    val monitoringActive: Boolean,
)
