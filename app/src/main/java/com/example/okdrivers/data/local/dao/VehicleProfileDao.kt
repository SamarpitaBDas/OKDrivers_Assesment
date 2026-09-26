package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.okdrivers.data.local.entity.VehicleProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleProfileEntity)
    @Query("SELECT * FROM vehicle_profiles")
    fun getAll(): Flow<List<VehicleProfileEntity>>
    @Query("SELECT * FROM vehicle_profiles WHERE id = :id")
    suspend fun getById(id: String): VehicleProfileEntity?
    @Query("SELECT * FROM vehicle_profiles WHERE id = :id")
    fun observeById(id: String): Flow<VehicleProfileEntity?>
}