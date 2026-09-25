package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_baselines")
data class SafetyBaselineEntity(
    @PrimaryKey
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val averageSpeedKmh: Float,
    val averageRpm: Int,
    val averageEngineLoad: Float,
    val averageBrakingG: Float,
    val maximumNormalGForce: Float,
    val sampleCount: Int,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long
)