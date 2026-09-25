package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.SensorSample
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun observeSamples(): Flow<List<SensorSample>>
    suspend fun getRecentSamples(limit: Int): List<SensorSample>
    suspend fun saveSample(sample: SensorSample)
    suspend fun clearSamples()
}