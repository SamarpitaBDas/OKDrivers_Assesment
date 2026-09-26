package com.example.okdrivers.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serviceStateDataStore by preferencesDataStore(name = "service_state_prefs")

@Singleton
class ServiceStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ServiceStateRepository {

    private object PreferencesKeys {
        val SERVICE_RUNNING = booleanPreferencesKey("is_service_running")
    }

    override val isServiceRunningFlow: Flow<Boolean> = context.serviceStateDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SERVICE_RUNNING] ?: true
        }

    override suspend fun setServiceRunning(running: Boolean) {
        context.serviceStateDataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVICE_RUNNING] = running
        }
    }
}
