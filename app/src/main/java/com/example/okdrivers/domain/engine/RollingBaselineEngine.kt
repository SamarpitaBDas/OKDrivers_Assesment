package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.SensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

class RollingBaselineEngine @Inject constructor() : BaselineEngine {

    override fun calculateDriverBaseline(
        samples: List<SensorSample>,
        now: Long
    ): BaselineStatistics {

        if (samples.isEmpty()) {
            return emptyBaseline(now)
        }

        val speeds = samples.map { it.speedKmh }
        val gForces = samples.map { it.gForce }

        return BaselineStatistics(
            sampleCount = samples.size,

            averageSpeedKmh = speeds.average().toFloat(),
            minimumSpeedKmh = speeds.minOrNull() ?: 0f,
            maximumSpeedKmh = speeds.maxOrNull() ?: 0f,
            speedStdDev = standardDeviation(speeds),

            averageGForce = gForces.average().toFloat(),
            minimumGForce = gForces.minOrNull() ?: 0f,
            maximumGForce = gForces.maxOrNull() ?: 0f,
            gForceStdDev = standardDeviation(gForces),

            averageRpm = 0f,
            minimumRpm = 0f,
            maximumRpm = 0f,
            rpmStdDev = 0f,

            averageEngineLoad = 0f,
            minimumEngineLoad = 0f,
            maximumEngineLoad = 0f,
            engineLoadStdDev = 0f,

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

        val speeds = samples.map { it.speedKmh }
        val gForces = emptyList<Float>()
        val rpms = samples.map { it.rpm }
        val engineLoads = samples.map { it.engineLoad }

        return BaselineStatistics(
            sampleCount = samples.size,

            averageSpeedKmh = speeds.average().toFloat(),
            minimumSpeedKmh = speeds.minOrNull() ?: 0f,
            maximumSpeedKmh = speeds.maxOrNull() ?: 0f,
            speedStdDev = standardDeviation(speeds),

            averageGForce = 0f,
            minimumGForce = 0f,
            maximumGForce = 0f,
            gForceStdDev = 0f,

            averageRpm = rpms.average().toFloat(),
            minimumRpm = rpms.minOrNull() ?: 0f,
            maximumRpm = rpms.maxOrNull() ?: 0f,
            rpmStdDev = standardDeviation(rpms),

            averageEngineLoad = engineLoads.average().toFloat(),
            minimumEngineLoad = engineLoads.minOrNull() ?: 0f,
            maximumEngineLoad = engineLoads.maxOrNull() ?: 0f,
            engineLoadStdDev = standardDeviation(engineLoads),

            updatedAt = now
        )
    }

    private fun standardDeviation(
        values: List<Float>
    ): Float {

        if (values.size < 2) {
            return 0f
        }

        val mean = values.average()

        val variance =
            values
                .map { (it - mean).toDouble().pow(2) }
                .average()

        return sqrt(variance).toFloat()
    }

    private fun emptyBaseline(
        now: Long
    ): BaselineStatistics {

        return BaselineStatistics(
            sampleCount = 0,

            averageSpeedKmh = 0f,
            minimumSpeedKmh = 0f,
            maximumSpeedKmh = 0f,
            speedStdDev = 0f,

            averageGForce = 0f,
            minimumGForce = 0f,
            maximumGForce = 0f,
            gForceStdDev = 0f,

            averageRpm = 0f,
            minimumRpm = 0f,
            maximumRpm = 0f,
            rpmStdDev = 0f,

            averageEngineLoad = 0f,
            minimumEngineLoad = 0f,
            maximumEngineLoad = 0f,
            engineLoadStdDev = 0f,

            updatedAt = now
        )
    }
}