package com.example.okdrivers.domain.engine

import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import javax.inject.Inject

class RollingBaselineEngine @Inject constructor() : BaselineEngine {

    override fun calculateDriverBaseline(
        samples: List<MotionSensorSample>,
        now: Long
    ): BaselineStatistics {

        if (samples.isEmpty()) {
            return emptyBaseline(now)
        }

        val gForces =
            samples.map { it.gForce }

        return BaselineStatistics(
            sampleCount = samples.size,

            averageSpeedKmh = 0f,
            minimumSpeedKmh = 0f,
            maximumSpeedKmh = 0f,

            averageGForce = gForces.average().toFloat(),
            minimumGForce = gForces.minOrNull() ?: 0f,
            maximumGForce = gForces.maxOrNull() ?: 0f,

            averageRpm = 0f,
            minimumRpm = 0f,
            maximumRpm = 0f,

            averageEngineLoad = 0f,
            minimumEngineLoad = 0f,
            maximumEngineLoad = 0f,

            updatedAt = now
        )
    }

    override fun calculateVehicleBaseline(
        samples: List<VehicleTelemetrySample>,
        now: Long
    ): BaselineStatistics {

        if (samples.isEmpty()) {
            return emptyBaseline(now)
        }

        val speeds =
            samples.map { it.speedKmh }

        val rpms =
            samples.map { it.rpm }

        val engineLoads =
            samples.map { it.engineLoad }

        return BaselineStatistics(
            sampleCount = samples.size,

            averageSpeedKmh =
                speeds.average().toFloat(),

            minimumSpeedKmh =
                speeds.minOrNull() ?: 0f,

            maximumSpeedKmh =
                speeds.maxOrNull() ?: 0f,

            averageGForce = 0f,
            minimumGForce = 0f,
            maximumGForce = 0f,

            averageRpm =
                rpms.average().toFloat(),

            minimumRpm =
                rpms.minOrNull() ?: 0f,

            maximumRpm =
                rpms.maxOrNull() ?: 0f,

            averageEngineLoad =
                engineLoads.average().toFloat(),

            minimumEngineLoad =
                engineLoads.minOrNull() ?: 0f,

            maximumEngineLoad =
                engineLoads.maxOrNull() ?: 0f,

            updatedAt = now
        )
    }

    private fun emptyBaseline(
        now: Long
    ): BaselineStatistics {
        return BaselineStatistics(
            sampleCount = 0,

            averageSpeedKmh = 0f,
            minimumSpeedKmh = 0f,
            maximumSpeedKmh = 0f,

            averageGForce = 0f,
            minimumGForce = 0f,
            maximumGForce = 0f,

            averageRpm = 0f,
            minimumRpm = 0f,
            maximumRpm = 0f,

            averageEngineLoad = 0f,
            minimumEngineLoad = 0f,
            maximumEngineLoad = 0f,

            updatedAt = now
        )
    }
}