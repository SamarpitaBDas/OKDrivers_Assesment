package com.example.okdrivers.domain.model

data class DriverProfile(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
    val createdAt: Long,
    val updatedAt: Long
)