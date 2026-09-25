package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.okdrivers.data.local.entity.ResponderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(responder: ResponderEntity)
    @Query("""
        SELECT * FROM responders
        WHERE isActive = 1
        ORDER BY distanceKm ASC
    """)
    fun observeActiveResponders(): Flow<List<ResponderEntity>>
}