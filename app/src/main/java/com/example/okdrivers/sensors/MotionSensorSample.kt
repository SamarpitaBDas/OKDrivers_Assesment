package com.example.okdrivers.sensors

data class MotionSensorSample(
    val timestamp: Long,
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val gForce: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)