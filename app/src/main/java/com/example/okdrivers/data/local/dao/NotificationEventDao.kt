package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.NotificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {
    @Insert
    suspend fun insert(event: NotificationEventEntity)
    @Query("""
        SELECT * FROM notification_events
        ORDER BY timestamp DESC
    """)
    fun observeAll(): Flow<List<NotificationEventEntity>>
}