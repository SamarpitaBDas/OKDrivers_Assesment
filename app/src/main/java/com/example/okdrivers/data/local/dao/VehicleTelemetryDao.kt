package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.VehicleTelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleTelemetryDao {
    @Insert
    suspend fun insert(telemetry: VehicleTelemetryEntity)
    @Query("SELECT * FROM vehicle_telemetry ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<VehicleTelemetryEntity>>
    @Query("SELECT * FROM vehicle_telemetry ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<VehicleTelemetryEntity>
}