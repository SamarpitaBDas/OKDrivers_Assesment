package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_profiles")
data class DriverProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
    val createdAt: Long,
    val updatedAt: Long
)