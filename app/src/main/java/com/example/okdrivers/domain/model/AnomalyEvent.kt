package com.example.okdrivers.domain.model

data class AnomalyEvent(
    val id: String,
    val timestamp: Long,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    // 0.0 - 1.0
    val confidence: Float,
    val reason: String,
    // Context
    val latitude: Double?,
    val longitude: Double?,
    val driverId: String?,
    val vehicleId: String?,
    val sensorSampleTimestamp: Long?,
    val telemetryTimestamp: Long?,
    val requiresVerification: Boolean,
    val isEscalated: Boolean
)

enum class AnomalyType {
    HARD_BRAKING,
    SEVERE_G_FORCE,
    RAPID_SPEED_DROP,
    ENGINE_STOP,
    CRITICAL_VEHICLE_ANOMALY,
    DRIVER_UNRESPONSIVE,
    MULTIPLE_ABNORMAL_SIGNALS,
    UNKNOWN
}

enum class AnomalySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}