package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.SensorSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorSampleDao {
    @Insert
    suspend fun insert(sample: SensorSampleEntity)
    @Query("SELECT * FROM sensor_samples ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SensorSampleEntity>>
    @Query("SELECT * FROM sensor_samples ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SensorSampleEntity>
    @Query("DELETE FROM sensor_samples")
    suspend fun deleteAll()
}