package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.Responder
import kotlinx.coroutines.flow.Flow

interface ResponderRepository {
    fun observeActiveResponders(): Flow<List<Responder>>
    suspend fun saveResponder(
        responder: Responder
    )
}