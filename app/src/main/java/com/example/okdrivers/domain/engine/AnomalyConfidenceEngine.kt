package com.example.okdrivers.domain.engine

import javax.inject.Inject

class AnomalyConfidenceEngine @Inject constructor() {

    fun calculateDriverGForceDeviation(
        currentGForce: Float,
        baseline: BaselineStatistics
    ): DeviationResult {
        if (baseline.sampleCount < 2) {
            return DeviationResult(
                deviationScore = 0f,
                confidence = 0f,
                isOutsideNormalRange = false
            )
        }
        return DeviationCalculator.computeDeviation(
            currentValue = currentGForce,
            mean = baseline.averageGForce,
            stdDev = baseline.gForceStdDev,
            minVal = baseline.minimumGForce,
            maxVal = baseline.maximumGForce
        )
    }

    fun calculateVehicleSpeedDeviation(
        currentSpeedKmh: Float,
        baseline: BaselineStatistics
    ): DeviationResult {
        if (baseline.sampleCount < 2) {
            return DeviationResult(
                deviationScore = 0f,
                confidence = 0f,
                isOutsideNormalRange = false
            )
        }
        return DeviationCalculator.computeDeviation(
            currentValue = currentSpeedKmh,
            mean = baseline.averageSpeedKmh,
            stdDev = baseline.speedStdDev,
            minVal = baseline.minimumSpeedKmh,
            maxVal = baseline.maximumSpeedKmh
        )
    }

    fun calculateVehicleRpmDeviation(
        currentRpm: Float,
        baseline: BaselineStatistics
    ): DeviationResult {
        if (baseline.sampleCount < 2) {
            return DeviationResult(
                deviationScore = 0f,
                confidence = 0f,
                isOutsideNormalRange = false
            )
        }
        return DeviationCalculator.computeDeviation(
            currentValue = currentRpm,
            mean = baseline.averageRpm,
            stdDev = baseline.rpmStdDev,
            minVal = baseline.minimumRpm,
            maxVal = baseline.maximumRpm
        )
    }

    fun combine(
        driverDeviation: DeviationResult,
        vehicleDeviations: List<DeviationResult>
    ): AnomalyConfidenceResult {
        val vehicleConfidence =
            if (vehicleDeviations.isEmpty()) {
                0f
            } else {
                vehicleDeviations
                    .map { it.confidence }
                    .average()
                    .toFloat()
            }

        val combinedConfidence =
            (
                    driverDeviation.confidence * 0.45f +
                            vehicleConfidence * 0.55f
                    ).coerceIn(0f, 1f)

        val driverAnomaly =
            driverDeviation.isOutsideNormalRange &&
                    driverDeviation.confidence >= 0.5f

        val vehicleAnomaly =
            vehicleDeviations.any {
                it.isOutsideNormalRange &&
                        it.confidence >= 0.5f
            }

        val combinedDeviation =
            (
                    driverDeviation.deviationScore +
                            vehicleDeviations.sumOf {
                                it.deviationScore.toDouble()
                            }.toFloat()
                    )

        return AnomalyConfidenceResult(
            driverDeviation = driverDeviation.deviationScore,
            vehicleDeviation = vehicleConfidence,
            combinedDeviation = combinedDeviation,
            anomalyConfidence = combinedConfidence,
            driverAnomaly = driverAnomaly,
            vehicleAnomaly = vehicleAnomaly
        )
    }
}
