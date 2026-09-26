package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.SafetyBaseline
import kotlinx.coroutines.flow.Flow

interface BaselineRepository {

    suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaseline?

    fun observeBaseline(
        driverId: String,
        vehicleId: String
    ): Flow<SafetyBaseline?>

    suspend fun saveBaseline(baseline: SafetyBaseline)
}