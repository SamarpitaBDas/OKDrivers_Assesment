package com.example.okdrivers.domain.model

data class Responder(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean,
    val reputationScore: Float?,
    val status: ResponderStatus
)

enum class ResponderStatus {
    AVAILABLE,
    NOTIFIED,
    ACCEPTED,
    ENROUTE,
    ARRIVED,
    DECLINED,
    UNAVAILABLE
}
