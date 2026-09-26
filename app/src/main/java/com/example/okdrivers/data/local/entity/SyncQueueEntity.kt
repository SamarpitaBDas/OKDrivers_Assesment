package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    val entityType: String,
    val entityId: String,
    val createdAt: Long,
    val synced: Boolean
)
