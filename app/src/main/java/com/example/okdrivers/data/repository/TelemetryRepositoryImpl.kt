package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.VehicleTelemetryDao
import com.example.okdrivers.domain.model.VehicleTelemetry
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TelemetryRepositoryImpl @Inject constructor(
    private val dao: VehicleTelemetryDao
) : TelemetryRepository {

    override fun observeTelemetry(): Flow<List<VehicleTelemetry>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getRecentTelemetry(
        limit: Int
    ): List<VehicleTelemetry> {
        return dao.getRecent(limit).map { it.toDomain() }
    }

    override suspend fun saveTelemetry(
        telemetry: VehicleTelemetry
    ) {
        dao.insert(telemetry.toEntity())
    }
}