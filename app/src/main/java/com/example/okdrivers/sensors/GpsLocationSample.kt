package com.example.okdrivers.sensors

data class GpsLocationSample(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMetersPerSecond: Float,
    val headingDegrees: Float
)