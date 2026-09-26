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

enum class SamplingRate(val intervalMillis: Long, val label: String) {
    NORMAL(1000L, "Normal (1Hz)"),
    BATTERY_SAVER(5000L, "Battery Saver (0.2Hz)")
}

private val Context.dataStore by preferencesDataStore(name = "sampling_prefs")

@Singleton
class SamplingRateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val SAMPLING_RATE = stringPreferencesKey("sampling_rate")
    }

    val samplingRateFlow: Flow<SamplingRate> = context.dataStore.data
        .map { preferences ->
            val rateName = preferences[PreferencesKeys.SAMPLING_RATE] ?: SamplingRate.NORMAL.name
            runCatching { SamplingRate.valueOf(rateName) }.getOrDefault(SamplingRate.NORMAL)
        }

    suspend fun setSamplingRate(rate: SamplingRate) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAMPLING_RATE] = rate.name
        }
    }
}
