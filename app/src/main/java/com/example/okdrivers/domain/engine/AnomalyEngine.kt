package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class AnomalyEngine @Inject constructor(
    private val anomalyConfidenceEngine: AnomalyConfidenceEngine
) {

    fun evaluate(
        driverId: String? = null,
        vehicleId: String? = null,
        vehicleTelemetry: VehicleTelemetrySample?,
        driverState: DriverState?,
        motionSensor: MotionSensorSample?,
        gpsLocation: GpsLocationSample?,
        driverBaseline: BaselineStatistics?,
        vehicleBaseline: BaselineStatistics?,
        recentHistory: List<AnomalyEvent> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): AnomalyDetectionResult {

        val events = mutableListOf<AnomalyEvent>()
        var maxConfidence = 0f
        var abnormalSignalCount = 0

        // 1. Motion & G-Force Analysis
        if (motionSensor != null && driverBaseline != null) {
            val gForceDeviation = anomalyConfidenceEngine.calculateDriverGForceDeviation(
                motionSensor.gForce,
                driverBaseline
            )

            val isOut = gForceDeviation.isOutsideNormalRange || motionSensor.gForce <= 0.85f || motionSensor.gForce >= 1.3f
            if (isOut) {
                abnormalSignalCount++
                val isHardBraking = motionSensor.gForce <= 0.85f || motionSensor.gForce < (driverBaseline.averageGForce - 0.15f) || motionSensor.gForce > (driverBaseline.averageGForce + 0.25f)
                val type = if (isHardBraking) AnomalyType.HARD_BRAKING else AnomalyType.SEVERE_G_FORCE
                val conf = max(gForceDeviation.confidence, 0.6f)
                val sev = if (isHardBraking && motionSensor.gForce <= 0.7f) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM
                val reason = if (isHardBraking) {
                    "Hard braking or acceleration detected with G-Force: %.2fG".format(motionSensor.gForce)
                } else {
                    "Severe G-Force deviation detected: %.2fG".format(motionSensor.gForce)
                }

                events.add(
                    AnomalyEvent(
                        id = UUID.randomUUID().toString(),
                        timestamp = now,
                        type = type,
                        severity = sev,
                        confidence = conf,
                        reason = reason,
                        latitude = gpsLocation?.latitude,
                        longitude = gpsLocation?.longitude,
                        driverId = driverId,
                        vehicleId = vehicleId,
                        sensorSampleTimestamp = motionSensor.timestamp,
                        telemetryTimestamp = vehicleTelemetry?.timestamp,
                        requiresVerification = sev >= AnomalySeverity.HIGH,
                        isEscalated = false
                    )
                )
                maxConfidence = max(maxConfidence, conf)
            }
        }

        // 2. Vehicle Telemetry Analysis
        if (vehicleTelemetry != null) {
            if (vehicleBaseline != null) {
                val speedDeviation = anomalyConfidenceEngine.calculateVehicleSpeedDeviation(
                    vehicleTelemetry.speedKmh,
                    vehicleBaseline
                )
                if (speedDeviation.isOutsideNormalRange && speedDeviation.confidence >= 0.5f) {
                    abnormalSignalCount++
                    val sev = AnomalySeverity.MEDIUM
                    events.add(
                        AnomalyEvent(
                            id = UUID.randomUUID().toString(),
                            timestamp = now,
                            type = AnomalyType.RAPID_SPEED_DROP,
                            severity = sev,
                            confidence = speedDeviation.confidence,
                            reason = "Rapid speed change detected: %.1f km/h".format(vehicleTelemetry.speedKmh),
                            latitude = gpsLocation?.latitude,
                            longitude = gpsLocation?.longitude,
                            driverId = driverId,
                            vehicleId = vehicleId,
                            sensorSampleTimestamp = null,
                            telemetryTimestamp = vehicleTelemetry.timestamp,
                            requiresVerification = false,
                            isEscalated = false
                        )
                    )
                    maxConfidence = max(maxConfidence, speedDeviation.confidence)
                }

                if (vehicleTelemetry.rpm == 0f && vehicleTelemetry.speedKmh > 20f) {
                    abnormalSignalCount++
                    val conf = 0.95f
                    val sev = AnomalySeverity.CRITICAL
                    events.add(
                        AnomalyEvent(
                            id = UUID.randomUUID().toString(),
                            timestamp = now,
                            type = AnomalyType.ENGINE_STOP,
                            severity = sev,
                            confidence = conf,
                            reason = "Engine stopped unexpectedly while moving at %.1f km/h".format(vehicleTelemetry.speedKmh),
                            latitude = gpsLocation?.latitude,
                            longitude = gpsLocation?.longitude,
                            driverId = driverId,
                            vehicleId = vehicleId,
                            sensorSampleTimestamp = null,
                            telemetryTimestamp = vehicleTelemetry.timestamp,
                            requiresVerification = true,
                            isEscalated = true
                        )
                    )
                    maxConfidence = max(maxConfidence, conf)
                }
            }

            if (!vehicleTelemetry.diagnosticFault.isNullOrBlank()) {
                abnormalSignalCount++
                val conf = 0.85f
                val sev = AnomalySeverity.HIGH
                events.add(
                    AnomalyEvent(
                        id = UUID.randomUUID().toString(),
                        timestamp = now,
                        type = AnomalyType.CRITICAL_VEHICLE_ANOMALY,
                        severity = sev,
                        confidence = conf,
                        reason = "Diagnostic fault reported: ${vehicleTelemetry.diagnosticFault}",
                        latitude = gpsLocation?.latitude,
                        longitude = gpsLocation?.longitude,
                        driverId = driverId,
                        vehicleId = vehicleId,
                        sensorSampleTimestamp = null,
                        telemetryTimestamp = vehicleTelemetry.timestamp,
                        requiresVerification = true,
                        isEscalated = false
                    )
                )
                maxConfidence = max(maxConfidence, conf)
            }
        }

        // 3. Driver State Analysis
        var driverAttentionFactor = 1.0f
        if (driverState != null) {
            when (driverState.condition) {
                DriverCondition.UNRESPONSIVE -> {
                    abnormalSignalCount++
                    val conf = 0.95f
                    val sev = AnomalySeverity.CRITICAL
                    events.add(
                        AnomalyEvent(
                            id = UUID.randomUUID().toString(),
                            timestamp = now,
                            type = AnomalyType.DRIVER_UNRESPONSIVE,
                            severity = sev,
                            confidence = conf,
                            reason = "Driver is unresponsive (Attention: %.2f)".format(driverState.attentionScore),
                            latitude = gpsLocation?.latitude,
                            longitude = gpsLocation?.longitude,
                            driverId = driverId,
                            vehicleId = vehicleId,
                            sensorSampleTimestamp = null,
                            telemetryTimestamp = vehicleTelemetry?.timestamp,
                            requiresVerification = true,
                            isEscalated = true
                        )
                    )
                    maxConfidence = max(maxConfidence, conf)
                    driverAttentionFactor = 1.5f
                }
                DriverCondition.DROWSY -> {
                    abnormalSignalCount++
                    val conf = 0.7f
                    val sev = AnomalySeverity.MEDIUM
                    events.add(
                        AnomalyEvent(
                            id = UUID.randomUUID().toString(),
                            timestamp = now,
                            type = AnomalyType.DRIVER_UNRESPONSIVE,
                            severity = sev,
                            confidence = conf,
                            reason = "Driver drowsiness detected (PERCLOS: %.2f)".format(driverState.perclos),
                            latitude = gpsLocation?.latitude,
                            longitude = gpsLocation?.longitude,
                            driverId = driverId,
                            vehicleId = vehicleId,
                            sensorSampleTimestamp = null,
                            telemetryTimestamp = vehicleTelemetry?.timestamp,
                            requiresVerification = true,
                            isEscalated = false
                        )
                    )
                    maxConfidence = max(maxConfidence, conf)
                    driverAttentionFactor = 1.2f
                }
                DriverCondition.DISTRACTED -> {
                    val conf = 0.6f
                    val sev = AnomalySeverity.LOW
                    events.add(
                        AnomalyEvent(
                            id = UUID.randomUUID().toString(),
                            timestamp = now,
                            type = AnomalyType.UNKNOWN,
                            severity = sev,
                            confidence = conf,
                            reason = "Driver distraction detected (Gaze away: %dms)".format(driverState.gazeAwayDurationMs),
                            latitude = gpsLocation?.latitude,
                            longitude = gpsLocation?.longitude,
                            driverId = driverId,
                            vehicleId = vehicleId,
                            sensorSampleTimestamp = null,
                            telemetryTimestamp = vehicleTelemetry?.timestamp,
                            requiresVerification = false,
                            isEscalated = false
                        )
                    )
                    maxConfidence = max(maxConfidence, conf)
                    driverAttentionFactor = 1.1f
                }
                else -> {}
            }
        }

        // 4. GPS & Time Context Analysis
        if (gpsLocation != null) {
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isNightTime = hour < 5 || hour > 22
            if (isNightTime && (driverState?.condition == DriverCondition.DROWSY || driverState?.condition == DriverCondition.DISTRACTED)) {
                maxConfidence = min(1.0f, maxConfidence * 1.15f)
            }
        }

        // 5. Recent History Compounding
        val recentWindowCount = recentHistory.count { now - it.timestamp < 30_000 }
        if (recentWindowCount > 0) {
            maxConfidence = min(1.0f, maxConfidence + (recentWindowCount * 0.1f))
        }

        // 6. Multiple Abnormal Signals Fusion
        if (abnormalSignalCount >= 2) {
            val conf = min(1.0f, maxConfidence * 1.2f)
            val sev = AnomalySeverity.HIGH
            events.add(
                AnomalyEvent(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    type = AnomalyType.MULTIPLE_ABNORMAL_SIGNALS,
                    severity = sev,
                    confidence = conf,
                    reason = "Multiple abnormal signals fused ($abnormalSignalCount signals detected)",
                    latitude = gpsLocation?.latitude,
                    longitude = gpsLocation?.longitude,
                    driverId = driverId,
                    vehicleId = vehicleId,
                    sensorSampleTimestamp = motionSensor?.timestamp,
                    telemetryTimestamp = vehicleTelemetry?.timestamp,
                    requiresVerification = true,
                    isEscalated = false
                )
            )
            maxConfidence = conf
        }

        val finalConfidence = (maxConfidence * driverAttentionFactor).coerceIn(0f, 1f)

        val overallSeverity = when {
            events.any { it.severity == AnomalySeverity.CRITICAL } -> AnomalySeverity.CRITICAL
            events.any { it.severity == AnomalySeverity.HIGH } -> AnomalySeverity.HIGH
            events.any { it.severity == AnomalySeverity.MEDIUM } -> AnomalySeverity.MEDIUM
            events.isNotEmpty() -> events.maxOf { it.severity }
            else -> AnomalySeverity.LOW
        }

        val isAlertDriver = driverState == null || driverState.condition == DriverCondition.ALERT || driverState.isResponsive
        val isStandardBrakingOnly = events.size == 1 && events.any { it.type == AnomalyType.HARD_BRAKING } && motionSensor != null && motionSensor.gForce <= 0.85f

        val requiresVerification = when {
            isStandardBrakingOnly && isAlertDriver -> false
            overallSeverity == AnomalySeverity.LOW -> false
            else -> true
        }

        val isEscalated = overallSeverity == AnomalySeverity.CRITICAL && finalConfidence >= 0.9f

        if (events.isEmpty()) {
            events.add(
                AnomalyEvent(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    type = AnomalyType.UNKNOWN,
                    severity = AnomalySeverity.LOW,
                    confidence = 0f,
                    reason = "Normal driving operation observed within expected baseline parameters",
                    latitude = gpsLocation?.latitude,
                    longitude = gpsLocation?.longitude,
                    driverId = driverId,
                    vehicleId = vehicleId,
                    sensorSampleTimestamp = motionSensor?.timestamp,
                    telemetryTimestamp = vehicleTelemetry?.timestamp,
                    requiresVerification = false,
                    isEscalated = false
                )
            )
        }

        return AnomalyDetectionResult(
            events = events.distinctBy { it.type },
            overallConfidence = finalConfidence,
            overallSeverity = overallSeverity,
            requiresVerification = requiresVerification,
            isEscalated = isEscalated
        )
    }
}
