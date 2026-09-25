package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.ResponderAction
import kotlinx.coroutines.flow.Flow

interface ResponderActionRepository {

    fun observeActions(
        incidentId: String
    ): Flow<List<ResponderAction>>

    suspend fun saveAction(
        action: ResponderAction
    )
}