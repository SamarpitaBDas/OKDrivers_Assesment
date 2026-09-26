package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.entity.AIConversationSessionEntity
import com.example.okdrivers.data.local.entity.AnomalyEventEntity
import com.example.okdrivers.data.local.entity.DriverProfileEntity
import com.example.okdrivers.data.local.entity.DriverStateEntity
import com.example.okdrivers.data.local.entity.IncidentEntity
import com.example.okdrivers.data.local.entity.IncidentStateTransitionEntity
import com.example.okdrivers.data.local.entity.NotificationEventEntity
import com.example.okdrivers.data.local.entity.ResponderActionEntity
import com.example.okdrivers.data.local.entity.ResponderEntity
import com.example.okdrivers.data.local.entity.SafetyBaselineEntity
import com.example.okdrivers.data.local.entity.SensorSampleEntity
import com.example.okdrivers.data.local.entity.VehicleProfileEntity
import com.example.okdrivers.data.local.entity.VehicleTelemetryEntity
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.DriverProfile
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.domain.model.NotificationEvent
import com.example.okdrivers.domain.model.NotificationRecipient
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.Responder
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import com.example.okdrivers.domain.model.ResponderStatus
import com.example.okdrivers.domain.model.SafetyBaseline
import com.example.okdrivers.domain.model.SensorSample
import com.example.okdrivers.domain.model.VehicleProfile
import com.example.okdrivers.domain.model.VehicleTelemetry

// DriverProfile

fun DriverProfileEntity.toDomain() = DriverProfile(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    emergencyContactName = emergencyContactName,
    emergencyContactPhone = emergencyContactPhone,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DriverProfile.toEntity() = DriverProfileEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    emergencyContactName = emergencyContactName,
    emergencyContactPhone = emergencyContactPhone,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// VehicleProfile

fun VehicleProfileEntity.toDomain() = VehicleProfile(
    id = id,
    registrationNumber = registrationNumber,
    make = make,
    model = model,
    year = year,
    driverId = driverId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun VehicleProfile.toEntity() = VehicleProfileEntity(
    id = id,
    registrationNumber = registrationNumber,
    make = make,
    model = model,
    year = year,
    driverId = driverId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// SensorSample

fun SensorSampleEntity.toDomain() = SensorSample(
    timestamp = timestamp,
    accelerationX = accelerationX,
    accelerationY = accelerationY,
    accelerationZ = accelerationZ,
    gForce = gForce,
    pitch = pitch,
    roll = roll,
    yaw = yaw,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    heading = heading,
    batteryPercentage = batteryPercentage,
    isCharging = isCharging,
    isNetworkOnline = isNetworkOnline
)

fun SensorSample.toEntity() = SensorSampleEntity(
    timestamp = timestamp,
    accelerationX = accelerationX,
    accelerationY = accelerationY,
    accelerationZ = accelerationZ,
    gForce = gForce,
    pitch = pitch,
    roll = roll,
    yaw = yaw,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    heading = heading,
    batteryPercentage = batteryPercentage,
    isCharging = isCharging,
    isNetworkOnline = isNetworkOnline
)

// VehicleTelemetry

fun VehicleTelemetryEntity.toDomain() = VehicleTelemetry(
    timestamp = timestamp,
    speedKmh = speedKmh,
    rpm = rpm,
    engineLoad = engineLoad,
    throttlePosition = throttlePosition,
    engineTemperature = engineTemperature,
    batteryVoltage = batteryVoltage,
    diagnosticFault = diagnosticFault
)

fun VehicleTelemetry.toEntity() = VehicleTelemetryEntity(
    timestamp = timestamp,
    speedKmh = speedKmh,
    rpm = rpm,
    engineLoad = engineLoad,
    throttlePosition = throttlePosition,
    engineTemperature = engineTemperature,
    batteryVoltage = batteryVoltage,
    diagnosticFault = diagnosticFault
)

// DriverState

fun DriverStateEntity.toDomain() = DriverState(
    timestamp = timestamp,
    attentionScore = attentionScore,
    perclos = perclos,
    blinkRate = blinkRate,
    yawnDetected = yawnDetected,
    gazeAwayDurationMs = gazeAwayDurationMs,
    headPitch = headPitch,
    headYaw = headYaw,
    headRoll = headRoll,
    isResponsive = isResponsive,
    condition = runCatching {
        DriverCondition.valueOf(condition)
    }.getOrDefault(DriverCondition.UNKNOWN),
    gazeDirection = gazeDirection
)

fun DriverState.toEntity() = DriverStateEntity(
    timestamp = timestamp,
    attentionScore = attentionScore,
    perclos = perclos,
    blinkRate = blinkRate,
    yawnDetected = yawnDetected,
    gazeAwayDurationMs = gazeAwayDurationMs,
    headPitch = headPitch,
    headYaw = headYaw,
    headRoll = headRoll,
    isResponsive = isResponsive,
    condition = condition.name,
    gazeDirection = gazeDirection
)

// SafetyBaseline

//fun SafetyBaselineEntity.toDomain() = SafetyBaseline(
//    id = id,
//    driverId = driverId,
//    vehicleId = vehicleId,
//    averageSpeedKmh = averageSpeedKmh,
//    averageRpm = averageRpm,
//    averageEngineLoad = averageEngineLoad,
//    averageBrakingG = averageBrakingG,
//    maximumNormalGForce = maximumNormalGForce,
//    sampleCount = sampleCount,
//    confidence = confidence,
//    createdAt = createdAt,
//    updatedAt = updatedAt
//)
//
//fun SafetyBaseline.toEntity() = SafetyBaselineEntity(
//    id = id,
//    driverId = driverId,
//    vehicleId = vehicleId,
//    averageSpeedKmh = averageSpeedKmh,
//    averageRpm = averageRpm,
//    averageEngineLoad = averageEngineLoad,
//    averageBrakingG = averageBrakingG,
//    maximumNormalGForce = maximumNormalGForce,
//    sampleCount = sampleCount,
//    confidence = confidence,
//    createdAt = createdAt,
//    updatedAt = updatedAt
//)

fun SafetyBaselineEntity.toDomain(): SafetyBaseline {
    return SafetyBaseline(
        id = id,
        driverId = driverId,
        vehicleId = vehicleId,
        averageSpeedKmh = averageSpeedKmh,
        averageRpm = averageRpm,
        averageEngineLoad = averageEngineLoad,
        averageBrakingG = averageBrakingG,
        maximumNormalGForce = maximumNormalGForce,
        sampleCount = sampleCount,
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SafetyBaseline.toEntity(): SafetyBaselineEntity {
    return SafetyBaselineEntity(
        id = id,
        driverId = driverId,
        vehicleId = vehicleId,
        averageSpeedKmh = averageSpeedKmh,
        averageRpm = averageRpm,
        averageEngineLoad = averageEngineLoad,
        averageBrakingG = averageBrakingG,
        maximumNormalGForce = maximumNormalGForce,
        sampleCount = sampleCount,
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
// AnomalyEvent

fun AnomalyEventEntity.toDomain() = AnomalyEvent(
    id = id,
    timestamp = timestamp,
    type = runCatching {
        AnomalyType.valueOf(type)
    }.getOrDefault(AnomalyType.UNKNOWN),
    severity = runCatching {
        AnomalySeverity.valueOf(severity)
    }.getOrDefault(AnomalySeverity.LOW),
    confidence = confidence,
    reason = reason,
    latitude = latitude,
    longitude = longitude,
    driverId = driverId,
    vehicleId = vehicleId,
    sensorSampleTimestamp = sensorSampleTimestamp,
    telemetryTimestamp = telemetryTimestamp,
    requiresVerification = requiresVerification,
    isEscalated = isEscalated
)

fun AnomalyEvent.toEntity() = AnomalyEventEntity(
    id = id,
    timestamp = timestamp,
    type = type.name,
    severity = severity.name,
    confidence = confidence,
    reason = reason,
    latitude = latitude,
    longitude = longitude,
    driverId = driverId,
    vehicleId = vehicleId,
    sensorSampleTimestamp = sensorSampleTimestamp,
    telemetryTimestamp = telemetryTimestamp,
    requiresVerification = requiresVerification,
    isEscalated = isEscalated
)

// Incident

fun IncidentEntity.toDomain() = Incident(
    id = id,
    driverId = driverId,
    vehicleId = vehicleId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    latitude = latitude,
    longitude = longitude,
    severity = runCatching {
        AnomalySeverity.valueOf(severity)
    }.getOrDefault(AnomalySeverity.LOW),
    currentState = runCatching {
        EmergencyState.valueOf(currentState)
    }.getOrDefault(EmergencyState.NORMAL_OPERATION),
    anomalyConfidence = anomalyConfidence,
    primaryAnomalyId = primaryAnomalyId,
    driverCondition = driverCondition?.let {
        runCatching { DriverCondition.valueOf(it) }
            .getOrDefault(DriverCondition.UNKNOWN)
    },
    communityMobilized = communityMobilized,
    responderAccepted = responderAccepted,
    authorityEscalated = authorityEscalated,
    resolvedAt = resolvedAt
)

fun Incident.toEntity() = IncidentEntity(
    id = id,
    driverId = driverId,
    vehicleId = vehicleId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    latitude = latitude,
    longitude = longitude,
    severity = severity.name,
    currentState = currentState.name,
    anomalyConfidence = anomalyConfidence,
    primaryAnomalyId = primaryAnomalyId,
    driverCondition = driverCondition?.name,
    communityMobilized = communityMobilized,
    responderAccepted = responderAccepted,
    authorityEscalated = authorityEscalated,
    resolvedAt = resolvedAt
)

// IncidentStateTransition

fun IncidentStateTransitionEntity.toDomain() = IncidentStateTransition(
    id = id,
    incidentId = incidentId,
    timestamp = timestamp,
    fromState = runCatching {
        EmergencyState.valueOf(fromState)
    }.getOrDefault(EmergencyState.NORMAL_OPERATION),
    toState = runCatching {
        EmergencyState.valueOf(toState)
    }.getOrDefault(EmergencyState.NORMAL_OPERATION),
    reason = reason
)

fun IncidentStateTransition.toEntity() = IncidentStateTransitionEntity(
    id = id,
    incidentId = incidentId,
    timestamp = timestamp,
    fromState = fromState.name,
    toState = toState.name,
    reason = reason
)

// AIConversationSession

fun AIConversationSessionEntity.toDomain() = AIConversationSession(
    id = id,
    incidentId = incidentId,
    startedAt = startedAt,
    endedAt = endedAt,
    prompt = prompt,
    response = response,
    responseClassification = responseClassification?.let {
        runCatching {
            ResponseClassification.valueOf(it)
        }.getOrDefault(ResponseClassification.UNRESPONSIVE)
    },
    responseLatencyMs = responseLatencyMs,
    attemptCount = attemptCount,
    completed = completed
)

fun AIConversationSession.toEntity() = AIConversationSessionEntity(
    id = id,
    incidentId = incidentId,
    startedAt = startedAt,
    endedAt = endedAt,
    prompt = prompt,
    response = response,
    responseClassification = responseClassification?.name,
    responseLatencyMs = responseLatencyMs,
    attemptCount = attemptCount,
    completed = completed
)

// Responder

fun ResponderEntity.toDomain() = Responder(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    isActive = isActive,
    reputationScore = reputationScore,
    status = runCatching {
        ResponderStatus.valueOf(status)
    }.getOrDefault(ResponderStatus.UNAVAILABLE)
)

fun Responder.toEntity() = ResponderEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    isActive = isActive,
    reputationScore = reputationScore,
    status = status.name
)

// ResponderAction

fun ResponderActionEntity.toDomain() = ResponderAction(
    id = id,
    incidentId = incidentId,
    responderId = responderId,
    timestamp = timestamp,
    action = runCatching {
        ResponderActionType.valueOf(action)
    }.getOrDefault(ResponderActionType.VIEWED),
    latitude = latitude,
    longitude = longitude
)

fun ResponderAction.toEntity() = ResponderActionEntity(
    id = id,
    incidentId = incidentId,
    responderId = responderId,
    timestamp = timestamp,
    action = action.name,
    latitude = latitude,
    longitude = longitude
)

// NotificationEvent

fun NotificationEventEntity.toDomain() = NotificationEvent(
    id = id,
    incidentId = incidentId,
    timestamp = timestamp,
    recipientType = runCatching {
        NotificationRecipient.valueOf(recipientType)
    }.getOrDefault(NotificationRecipient.SYSTEM),
    title = title,
    message = message,
    delivered = delivered,
    acknowledged = acknowledged
)

fun NotificationEvent.toEntity() = NotificationEventEntity(
    id = id,
    incidentId = incidentId,
    timestamp = timestamp,
    recipientType = recipientType.name,
    title = title,
    message = message,
    delivered = delivered,
    acknowledged = acknowledged
)