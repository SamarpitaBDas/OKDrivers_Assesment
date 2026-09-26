package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.AnomalyEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnomalyEventDao {
    @Insert
    suspend fun insert(event: AnomalyEventEntity)
    @Query("SELECT * FROM anomaly_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AnomalyEventEntity>>
    @Query("SELECT * FROM anomaly_events WHERE id = :id")
    suspend fun getById(id: String): AnomalyEventEntity?
}