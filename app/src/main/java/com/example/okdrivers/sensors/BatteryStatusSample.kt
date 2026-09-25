package com.example.okdrivers.sensors

data class BatteryStatusSample(
    val timestamp: Long,
    val batteryPercentage: Int,
    val isCharging: Boolean
)