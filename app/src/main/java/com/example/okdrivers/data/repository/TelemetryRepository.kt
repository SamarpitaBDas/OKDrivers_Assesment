package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.VehicleTelemetry
import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    fun observeTelemetry(): Flow<List<VehicleTelemetry>>
    suspend fun getRecentTelemetry(limit: Int): List<VehicleTelemetry>
    suspend fun saveTelemetry(telemetry: VehicleTelemetry)
}