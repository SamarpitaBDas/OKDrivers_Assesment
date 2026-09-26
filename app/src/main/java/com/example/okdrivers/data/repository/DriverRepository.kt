package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.DriverProfile
import kotlinx.coroutines.flow.Flow

interface DriverRepository {
    fun observeDrivers(): Flow<List<DriverProfile>>
    fun observeDriver(id: String): Flow<DriverProfile?>
    suspend fun getDriver(id: String): DriverProfile?
    suspend fun saveDriver(driver: DriverProfile)
}