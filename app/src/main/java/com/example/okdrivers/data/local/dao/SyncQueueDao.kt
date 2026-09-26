package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.okdrivers.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedItems(): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun observeQueue(): Flow<List<SyncQueueEntity>>
}
