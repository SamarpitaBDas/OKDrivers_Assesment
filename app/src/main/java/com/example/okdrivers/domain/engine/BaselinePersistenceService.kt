package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.BaselineRepository
import com.example.okdrivers.domain.model.SafetyBaseline
import javax.inject.Inject

class BaselinePersistenceService @Inject constructor(
    private val baselineRepository: BaselineRepository
) {

    suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaseline? {
        return baselineRepository.getBaseline(
            driverId = driverId,
            vehicleId = vehicleId
        )
    }

    suspend fun saveBaseline(
        baseline: SafetyBaseline
    ) {
        baselineRepository.saveBaseline(baseline)
    }
}