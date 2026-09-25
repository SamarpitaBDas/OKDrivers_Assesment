package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.AnomalyEvent
import kotlinx.coroutines.flow.Flow

interface AnomalyRepository {
    fun observeAnomalies(): Flow<List<AnomalyEvent>>
    suspend fun saveAnomaly(anomaly: AnomalyEvent)
}