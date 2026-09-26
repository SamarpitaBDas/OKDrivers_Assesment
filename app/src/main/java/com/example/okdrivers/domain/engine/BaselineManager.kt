package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.BaselineRepository
import com.example.okdrivers.domain.model.SafetyBaseline
import com.example.okdrivers.domain.model.SensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import javax.inject.Inject

class BaselineManager @Inject constructor(
    private val baselineRepository: BaselineRepository,
    private val adaptiveBaselineEngine: AdaptiveBaselineEngine
) {

    suspend fun updateDriverBaseline(
        driverId: String,
        vehicleId: String,
        sample: SensorSample,
        allowUpdate: Boolean = true
    ): BaselineUpdateResult {

        val existingBaseline =
            baselineRepository.getBaseline(
                driverId = driverId,
                vehicleId = vehicleId
            )

        val previousStatistics =
            existingBaseline?.toBaselineStatistics()

        val result =
            adaptiveBaselineEngine.updateDriverBaseline(
                currentSample = sample,
                previousBaseline = previousStatistics,
                allowUpdate = allowUpdate
            )

        if (result.updated) {
            val safetyBaseline =
                result.baseline.toSafetyBaseline(
                    driverId = driverId,
                    vehicleId = vehicleId,
                    existingBaseline = existingBaseline
                )

            baselineRepository.saveBaseline(safetyBaseline)
        }

        return result
    }

    suspend fun updateVehicleBaseline(
        driverId: String,
        vehicleId: String,
        sample: VehicleTelemetrySample,
        allowUpdate: Boolean = true
    ): BaselineUpdateResult {

        val existingBaseline =
            baselineRepository.getBaseline(
                driverId = driverId,
                vehicleId = vehicleId
            )

        val previousStatistics =
            existingBaseline?.toBaselineStatistics()

        val result =
            adaptiveBaselineEngine.updateVehicleBaseline(
                currentSample = sample,
                previousBaseline = previousStatistics,
                allowUpdate = allowUpdate
            )

        if (result.updated) {
            val safetyBaseline =
                result.baseline.toSafetyBaseline(
                    driverId = driverId,
                    vehicleId = vehicleId,
                    existingBaseline = existingBaseline
                )

            baselineRepository.saveBaseline(safetyBaseline)
        }

        return result
    }

    suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaseline? {
        return baselineRepository.getBaseline(
            driverId = driverId,
            vehicleId = vehicleId
        )
    }
}