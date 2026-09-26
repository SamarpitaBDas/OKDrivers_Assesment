package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.SafetyBaselineDao
import com.example.okdrivers.domain.model.SafetyBaseline
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BaselineRepositoryImpl @Inject constructor(
    private val dao: SafetyBaselineDao
) : BaselineRepository {

    override suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaseline? {
        return dao.getBaseline(driverId, vehicleId)?.toDomain()
    }

    override fun observeBaseline(
        driverId: String,
        vehicleId: String
    ): Flow<SafetyBaseline?> {
        return dao.observeBaseline(driverId, vehicleId).map { it?.toDomain() }
    }

    override suspend fun saveBaseline(
        baseline: SafetyBaseline
    ) {
        dao.insert(baseline.toEntity())
    }
}