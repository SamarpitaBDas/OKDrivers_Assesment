package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.IncidentStateTransitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentStateTransitionDao {
    @Insert
    suspend fun insert(transition: IncidentStateTransitionEntity)
    @Query("""
        SELECT * FROM incident_state_transitions
        WHERE incidentId = :incidentId
        ORDER BY timestamp ASC
    """)
    fun observeForIncident(
        incidentId: String
    ): Flow<List<IncidentStateTransitionEntity>>
}