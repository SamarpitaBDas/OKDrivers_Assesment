package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.NotificationRepository
import com.example.okdrivers.domain.model.NotificationEvent
import com.example.okdrivers.domain.model.NotificationRecipient
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyContactNotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    suspend fun notifyFamilyContact(
        incidentId: String,
        severity: String,
        locationSummary: String,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return runCatching {
            val message = "Emergency Alert: Your emergency contact has been involved in an incident (Severity: $severity). Location: $locationSummary"
            println("FAMILY CONTACT NOTIFICATION [Incident $incidentId]: $message")

            val notification = NotificationEvent(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                timestamp = now,
                recipientType = NotificationRecipient.FAMILY_CONTACT,
                title = "Emergency Contact Alert",
                message = message,
                delivered = true,
                acknowledged = false
            )
            notificationRepository.saveNotification(notification)
        }
    }
}
