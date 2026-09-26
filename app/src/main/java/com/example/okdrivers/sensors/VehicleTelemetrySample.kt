package com.example.okdrivers.sensors

data class VehicleTelemetrySample(
    val timestamp: Long,
    val speedKmh: Float,
    val rpm: Float,
    val engineLoad: Float,
    val throttlePosition: Float,
    val engineTemperatureCelsius: Float,
    val batteryVoltage: Float,
    val diagnosticFault: String?,
    val airbagDeployed: Boolean = false
)
