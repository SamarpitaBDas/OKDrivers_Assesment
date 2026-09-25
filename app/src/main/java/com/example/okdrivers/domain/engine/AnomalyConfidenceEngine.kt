package com.example.okdrivers.domain.engine

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AnomalyConfidenceEngine @Inject constructor() {

    companion object {
        private const val MIN_STANDARD_DEVIATION = 0.05f

        private const val NORMAL_Z_SCORE = 1.0f
        private const val ANOMALY_Z_SCORE = 3.0f
    }

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

        val standardDeviation =
            max(
                baseline.gForceStdDev,
                MIN_STANDARD_DEVIATION
            )

        val zScore =
            abs(
                currentGForce - baseline.averageGForce
            ) / standardDeviation

        val confidence =
            normalizeZScore(zScore)

        val outsideRange =
            currentGForce < baseline.minimumGForce ||
                    currentGForce > baseline.maximumGForce

        return DeviationResult(
            deviationScore = zScore,
            confidence = confidence,
            isOutsideNormalRange = outsideRange
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

        val standardDeviation =
            max(
                baseline.speedStdDev,
                MIN_STANDARD_DEVIATION
            )

        val zScore =
            abs(
                currentSpeedKmh - baseline.averageSpeedKmh
            ) / standardDeviation

        return DeviationResult(
            deviationScore = zScore,
            confidence = normalizeZScore(zScore),
            isOutsideNormalRange =
                currentSpeedKmh < baseline.minimumSpeedKmh ||
                        currentSpeedKmh > baseline.maximumSpeedKmh
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

        val standardDeviation =
            max(
                baseline.rpmStdDev,
                MIN_STANDARD_DEVIATION
            )

        val zScore =
            abs(
                currentRpm - baseline.averageRpm
            ) / standardDeviation

        return DeviationResult(
            deviationScore = zScore,
            confidence = normalizeZScore(zScore),
            isOutsideNormalRange =
                currentRpm < baseline.minimumRpm ||
                        currentRpm > baseline.maximumRpm
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

    private fun normalizeZScore(
        zScore: Float
    ): Float {

        return when {
            zScore <= NORMAL_Z_SCORE -> 0f

            zScore >= ANOMALY_Z_SCORE -> 1f

            else ->
                (
                        (zScore - NORMAL_Z_SCORE) /
                                (ANOMALY_Z_SCORE - NORMAL_Z_SCORE)
                        ).coerceIn(0f, 1f)
        }
    }
}