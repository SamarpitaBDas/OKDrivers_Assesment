package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.NotificationEventDao
import com.example.okdrivers.domain.model.NotificationEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationEventDao
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<NotificationEvent>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveNotification(
        notification: NotificationEvent
    ) {
        dao.insert(notification.toEntity())
    }
}