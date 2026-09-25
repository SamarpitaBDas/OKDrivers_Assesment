package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anomaly_events")
data class AnomalyEventEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val type: String,
    val severity: String,
    val confidence: Float,
    val reason: String,
    val latitude: Double?,
    val longitude: Double?,
    val driverId: String?,
    val vehicleId: String?,
    val sensorSampleTimestamp: Long?,
    val telemetryTimestamp: Long?,
    val requiresVerification: Boolean,
    val isEscalated: Boolean
)