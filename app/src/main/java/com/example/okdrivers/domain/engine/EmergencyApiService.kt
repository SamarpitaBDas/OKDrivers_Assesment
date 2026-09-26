package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.NotificationRepository
import com.example.okdrivers.domain.model.NotificationEvent
import com.example.okdrivers.domain.model.NotificationRecipient
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyApiService @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    suspend fun sendPayload(payload: IncidentPayload, now: Long = System.currentTimeMillis()): Result<Unit> {
        return runCatching {
            // Log simulated emergency API dispatch
            println("EMERGENCY API DISPATCH [${payload.incidentId}]: ${payload.summaryText}")

            // Insert notification event into Room so it's recorded as a dispatched notification to emergency services
            val notification = NotificationEvent(
                id = UUID.randomUUID().toString(),
                incidentId = payload.incidentId,
                timestamp = now,
                recipientType = NotificationRecipient.EMERGENCY_AUTHORITY,
                title = "Emergency Dispatched (Severity: ${payload.severity})",
                message = payload.summaryText,
                delivered = true,
                acknowledged = false
            )
            notificationRepository.saveNotification(notification)
        }
    }
}
