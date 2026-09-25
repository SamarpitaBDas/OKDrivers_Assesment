package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.ResponderActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponderActionDao {
    @Insert
    suspend fun insert(action: ResponderActionEntity)
    @Query("""
        SELECT * FROM responder_actions
        WHERE incidentId = :incidentId
        ORDER BY timestamp ASC
    """)
    fun observeForIncident(
        incidentId: String
    ): Flow<List<ResponderActionEntity>>
}