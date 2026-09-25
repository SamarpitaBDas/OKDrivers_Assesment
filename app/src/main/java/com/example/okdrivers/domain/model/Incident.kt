package com.example.okdrivers.domain.model

data class Incident(
    val id: String,
    val driverId: String?,
    val vehicleId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val severity: AnomalySeverity,
    val currentState: EmergencyState,
    val anomalyConfidence: Float,
    val primaryAnomalyId: String?,
    val driverCondition: DriverCondition?,
    val communityMobilized: Boolean,
    val responderAccepted: Boolean,
    val authorityEscalated: Boolean,
    val resolvedAt: Long?
)

enum class EmergencyState {
    NORMAL_OPERATION,
    ANOMALY_DETECTION,
    AI_VERIFICATION,
    COMMUNITY_MOBILIZATION,
    COMMUNITY_RESPONSE,
    AUTHORITY_ESCALATION
}