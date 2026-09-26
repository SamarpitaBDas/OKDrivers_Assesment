package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.SyncQueueDao
import com.example.okdrivers.data.local.entity.SyncQueueEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueRepository @Inject constructor(
    private val syncQueueDao: SyncQueueDao
) {

    suspend fun enqueue(entityType: String, entityId: String, now: Long = System.currentTimeMillis()) {
        val item = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            createdAt = now,
            synced = false
        )
        syncQueueDao.insert(item)
    }

    suspend fun getUnsyncedItems(): List<SyncQueueEntity> {
        return syncQueueDao.getUnsyncedItems()
    }

    suspend fun markSynced(id: String) {
        syncQueueDao.markSynced(id)
    }
}
