package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.SafetyBaselineDao
import com.example.okdrivers.domain.model.SafetyBaseline
import javax.inject.Inject

class BaselineRepositoryImpl @Inject constructor(
    private val dao: SafetyBaselineDao
) : BaselineRepository {

    override suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaseline? {
        return dao.getBaseline(driverId, vehicleId)?.toDomain()
    }

    override suspend fun saveBaseline(
        baseline: SafetyBaseline
    ) {
        dao.insert(baseline.toEntity())
    }
}