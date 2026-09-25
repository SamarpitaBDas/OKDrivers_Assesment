package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
    val id: String,
    val driverId: String?,
    val vehicleId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val severity: String,
    val currentState: String,
    val anomalyConfidence: Float,
    val primaryAnomalyId: String?,
    val driverCondition: String?,
    val communityMobilized: Boolean,
    val responderAccepted: Boolean,
    val authorityEscalated: Boolean,
    val resolvedAt: Long?
)