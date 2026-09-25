package com.example.okdrivers.domain.engine

data class AnomalyConfidenceResult(
    val driverDeviation: Float,
    val vehicleDeviation: Float,
    val combinedDeviation: Float,
    val anomalyConfidence: Float,
    val driverAnomaly: Boolean,
    val vehicleAnomaly: Boolean
)