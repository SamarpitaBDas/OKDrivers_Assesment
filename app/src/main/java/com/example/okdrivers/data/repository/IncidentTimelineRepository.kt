package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.IncidentStateTransition
import kotlinx.coroutines.flow.Flow

interface IncidentTimelineRepository {

    fun observeTimeline(
        incidentId: String
    ): Flow<List<IncidentStateTransition>>

    suspend fun saveTransition(
        transition: IncidentStateTransition
    )
}