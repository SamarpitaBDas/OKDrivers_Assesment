package com.example.okdrivers.domain.model

data class VehicleTelemetry(
    val timestamp: Long,
    val speedKmh: Float,
    val rpm: Int,
    val engineLoad: Float,
    val throttlePosition: Float,
    val engineTemperature: Float,
    val batteryVoltage: Float,
    val diagnosticFault: String?
)