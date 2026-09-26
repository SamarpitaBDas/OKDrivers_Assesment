package com.example.okdrivers.data.repository

import kotlinx.coroutines.flow.Flow

interface ServiceStateRepository {
    val isServiceRunningFlow: Flow<Boolean>
    suspend fun setServiceRunning(running: Boolean)
}
