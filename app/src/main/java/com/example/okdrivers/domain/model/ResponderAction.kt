package com.example.okdrivers.domain.model

data class ResponderAction(
    val id: String,
    val incidentId: String,
    val responderId: String,
    val timestamp: Long,
    val action: ResponderActionType,
    val latitude: Double?,
    val longitude: Double?
)

enum class ResponderActionType {
    NOTIFIED,
    VIEWED,
    ACCEPTED,
    DECLINED,
    ENROUTE,
    ARRIVED,
    CANCELLED
}