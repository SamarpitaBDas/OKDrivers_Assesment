package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.NotificationEvent
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<NotificationEvent>>
    suspend fun saveNotification(
        notification: NotificationEvent
    )
}