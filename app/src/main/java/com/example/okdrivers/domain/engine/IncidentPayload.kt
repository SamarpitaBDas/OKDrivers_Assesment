package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.VehicleTelemetry
import java.util.Locale

data class CommunityResponseStatus(
    val mobilized: Boolean,
    val respondersNotifiedCount: Int,
    val radiusReachedKm: Double?,
    val accepted: Boolean,
    val acceptedResponderName: String?
)

data class IncidentPayload(
    val incidentId: String,
    val timestamp: Long,
    val severity: AnomalySeverity,
    val location: Pair<Double, Double>?,
    val vehicleId: String?,
    val driverId: String?,
    val latestTelemetry: VehicleTelemetry?,
    val latestDmsSnapshot: DriverState?,
    val verificationOutcome: ResponseClassification?,
    val communityResponseStatus: CommunityResponseStatus,
    val summaryText: String
) {
    companion object {
        fun generateSummary(
            incidentId: String,
            severity: AnomalySeverity,
            location: Pair<Double, Double>?,
            vehicleId: String?,
            verificationOutcome: ResponseClassification?,
            communityStatus: CommunityResponseStatus
        ): String {
            val locStr = if (location != null) String.format(Locale.US, "Lat: %.4f, Lng: %.4f", location.first, location.second) else "Location unknown"
            val verifStr = verificationOutcome?.name ?: "No verification recorded"
            val commStr = if (communityStatus.accepted) "Accepted by ${communityStatus.acceptedResponderName}" else "Not accepted (Notified: ${communityStatus.respondersNotifiedCount})"
            return "INCIDENT [${incidentId.take(8)}] | Severity: $severity | Vehicle: ${vehicleId ?: "Unknown"} | $locStr | Verification: $verifStr | Community: $commStr"
        }
    }
}
