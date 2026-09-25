package com.example.okdrivers.domain.model

data class SafetyBaseline(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    // Normal driving behaviour
    val averageSpeedKmh: Float,
    val averageRpm: Int,
    val averageEngineLoad: Float,
    // Braking behaviour
    val averageBrakingG: Float,
    val maximumNormalGForce: Float,
    // Confidence and history
    val sampleCount: Int,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long
)