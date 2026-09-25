package com.example.okdrivers.sensors

data class SensorSnapshot(
    val motion: MotionSensorSample? = null,
    val gps: GpsLocationSample? = null,
    val battery: BatteryStatusSample? = null,
    val network: NetworkStatusSample? = null,
    val vehicleTelemetry: VehicleTelemetrySample? = null,
    val dms: DmsSample? = null
)