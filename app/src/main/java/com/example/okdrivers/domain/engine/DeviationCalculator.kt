package com.example.okdrivers.domain.engine

import kotlin.math.abs
import kotlin.math.max

object DeviationCalculator {
    private const val MIN_STANDARD_DEVIATION = 0.05f
    private const val NORMAL_Z_SCORE = 1.0f
    private const val ANOMALY_Z_SCORE = 3.0f

    fun computeDeviation(
        currentValue: Float,
        mean: Float,
        stdDev: Float,
        minVal: Float? = null,
        maxVal: Float? = null
    ): DeviationResult {
        val safeStdDev = max(stdDev, MIN_STANDARD_DEVIATION)
        val zScore = abs(currentValue - mean) / safeStdDev
        val confidence = normalizeZScore(zScore)

        val outsideRangeByMinMax = (minVal != null && currentValue < minVal) || (maxVal != null && currentValue > maxVal)
        val outsideRangeByZ = zScore >= NORMAL_Z_SCORE
        val isOutsideNormalRange = outsideRangeByMinMax || outsideRangeByZ

        return DeviationResult(
            deviationScore = zScore,
            confidence = confidence,
            isOutsideNormalRange = isOutsideNormalRange
        )
    }

    private fun normalizeZScore(zScore: Float): Float {
        return when {
            zScore <= NORMAL_Z_SCORE -> 0f
            zScore >= ANOMALY_Z_SCORE -> 1f
            else -> ((zScore - NORMAL_Z_SCORE) / (ANOMALY_Z_SCORE - NORMAL_Z_SCORE)).coerceIn(0f, 1f)
        }
    }
}
