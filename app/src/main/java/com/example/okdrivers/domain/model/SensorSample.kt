package com.example.okdrivers.domain.model

data class SensorSample(
    val timestamp: Long,
    // Accelerometer
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val gForce: Float,
    // Gyroscope/orientation
    val pitch: Float,
    val roll: Float,
    val yaw: Float,
    // GPS
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val heading: Float,
    // Device state
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val isNetworkOnline: Boolean
)