package com.example.okdrivers.sensors.test

data class SensorTestUiState(
    val isRunning: Boolean = false,

    val accelerometerAvailable: Boolean = false,
    val gyroscopeAvailable: Boolean = false,

    val accelerationX: Float = 0f,
    val accelerationY: Float = 0f,
    val accelerationZ: Float = 0f,

    val gForce: Float = 0f,

    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,

    val lastUpdated: Long? = null,

    val error: String? = null
)