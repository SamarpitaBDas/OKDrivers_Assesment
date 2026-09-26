package com.example.okdrivers.data.repository

import kotlinx.coroutines.flow.Flow

interface CurrentProfileRepository {
    fun observeCurrentDriverId(): Flow<String>
    fun observeCurrentVehicleId(): Flow<String>
    suspend fun setCurrentDriverId(driverId: String)
    suspend fun setCurrentVehicleId(vehicleId: String)
}
