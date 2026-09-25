package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.DriverStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverStateDao {
    @Insert
    suspend fun insert(state: DriverStateEntity)
    @Query("SELECT * FROM driver_states ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DriverStateEntity>>
    @Query("SELECT * FROM driver_states ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): DriverStateEntity?
}