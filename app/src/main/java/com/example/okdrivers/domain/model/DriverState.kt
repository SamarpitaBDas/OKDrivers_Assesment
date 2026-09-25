package com.example.okdrivers.domain.model

data class DriverState(
    val timestamp: Long,
    // alertness
    val attentionScore: Float,
    val perclos: Float,
    // face behaviour
    val blinkRate: Float,
    val yawnDetected: Boolean,
    val gazeAwayDurationMs: Long,
    // Head pose
    val headPitch: Float,
    val headYaw: Float,
    val headRoll: Float,
    // Responsiveness
    val isResponsive: Boolean,
    // Overall classification
    val condition: DriverCondition
)

enum class DriverCondition {
    ALERT,
    DISTRACTED,
    DROWSY,
    UNRESPONSIVE,
    UNKNOWN
}