package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun observeIncidents(): Flow<List<Incident>>
    fun observeActiveIncident(): Flow<Incident?>
    suspend fun getIncident(id: String): Incident?
    suspend fun saveIncident(incident: Incident)
}