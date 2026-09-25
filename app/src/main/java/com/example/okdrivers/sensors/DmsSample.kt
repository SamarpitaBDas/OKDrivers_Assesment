package com.example.okdrivers.sensors

data class DmsSample(
    val timestamp: Long,

    val perclos: Float,

    val gazeDirection: GazeDirection,

    val headPitch: Float,
    val headYaw: Float,
    val headRoll: Float,

    val blinkRate: Float,
    val yawnDetected: Boolean,

    val gazeAwayDurationMs: Long,

    val attentionScore: Float,

    val isResponsive: Boolean,

    val condition: DriverCondition
)

enum class GazeDirection {
    FORWARD,
    LEFT,
    RIGHT,
    UP,
    DOWN
}

enum class DriverCondition {
    ALERT,
    DISTRACTED,
    DROWSY,
    UNRESPONSIVE
}