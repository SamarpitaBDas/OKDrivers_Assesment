package com.example.okdrivers.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore(name = "profile_prefs")

@Singleton
class CurrentProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CurrentProfileRepository {

    private object PreferencesKeys {
        val DRIVER_ID = stringPreferencesKey("current_driver_id")
        val VEHICLE_ID = stringPreferencesKey("current_vehicle_id")
    }

    override fun observeCurrentDriverId(): Flow<String> = context.profileDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DRIVER_ID] ?: "driver_1"
        }

    override fun observeCurrentVehicleId(): Flow<String> = context.profileDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.VEHICLE_ID] ?: "vehicle_1"
        }

    override suspend fun setCurrentDriverId(driverId: String) {
        context.profileDataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVER_ID] = driverId
        }
    }

    override suspend fun setCurrentVehicleId(vehicleId: String) {
        context.profileDataStore.edit { preferences ->
            preferences[PreferencesKeys.VEHICLE_ID] = vehicleId
        }
    }
}
