package com.example.okdrivers.domain.model

data class NotificationEvent(
    val id: String,
    val incidentId: String?,
    val timestamp: Long,
    val recipientType: NotificationRecipient,
    val title: String,
    val message: String,
    val delivered: Boolean,
    val acknowledged: Boolean
)

enum class NotificationRecipient {
    DRIVER,
    RESPONDER,
    FAMILY_CONTACT,
    EMERGENCY_AUTHORITY,
    SYSTEM
}