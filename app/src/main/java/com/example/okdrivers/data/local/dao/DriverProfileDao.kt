package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.okdrivers.data.local.entity.DriverProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(driver: DriverProfileEntity)
    @Query("SELECT * FROM driver_profiles")
    fun getAll(): Flow<List<DriverProfileEntity>>
    @Query("SELECT * FROM driver_profiles WHERE id = :id")
    suspend fun getById(id: String): DriverProfileEntity?
}