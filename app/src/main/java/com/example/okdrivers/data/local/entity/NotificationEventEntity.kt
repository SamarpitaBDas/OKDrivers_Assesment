package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_events")
data class NotificationEventEntity(
    @PrimaryKey
    val id: String,
    val incidentId: String?,
    val timestamp: Long,
    val recipientType: String,
    val title: String,
    val message: String,
    val delivered: Boolean,
    val acknowledged: Boolean
)