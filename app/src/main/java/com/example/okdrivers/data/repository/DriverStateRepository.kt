package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.DriverState
import kotlinx.coroutines.flow.Flow

interface DriverStateRepository {
    fun observeStates(): Flow<List<DriverState>>
    suspend fun getLatestState(): DriverState?
    suspend fun saveState(state: DriverState)
}