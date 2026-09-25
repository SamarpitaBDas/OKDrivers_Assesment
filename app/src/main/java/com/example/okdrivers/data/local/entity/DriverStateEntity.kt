package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_states")
data class DriverStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val attentionScore: Float,
    val perclos: Float,
    val blinkRate: Float,
    val yawnDetected: Boolean,
    val gazeAwayDurationMs: Long,
    val headPitch: Float,
    val headYaw: Float,
    val headRoll: Float,
    val isResponsive: Boolean,
    val condition: String
)

//DriverCondition.valueOf(entity.condition)