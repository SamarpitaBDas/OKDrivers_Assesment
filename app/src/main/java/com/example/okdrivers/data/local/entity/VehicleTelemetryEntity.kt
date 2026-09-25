package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_telemetry")
data class VehicleTelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val speedKmh: Float,
    val rpm: Int,
    val engineLoad: Float,
    val throttlePosition: Float,
    val engineTemperature: Float,
    val batteryVoltage: Float,
    val diagnosticFault: String?
)