package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import com.example.okdrivers.domain.model.AnomalyEvent
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class AnomalyEngine @Inject constructor(
    private val anomalyConfidenceEngine: AnomalyConfidenceEngine
) {

    fun evaluate(
        vehicleTelemetry: VehicleTelemetrySample?,
        driverState: DriverState?,
        motionSensor: MotionSensorSample?,
        gpsLocation: GpsLocationSample?,
        driverBaseline: BaselineStatistics?,
        vehicleBaseline: BaselineStatistics?,
        recentHistory: List<AnomalyEvent> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): AnomalyDetectionResult {

        val classifications = mutableListOf<AnomalyType>()
        val reasoning = mutableListOf<String>()

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
                if (isHardBraking) {
                    classifications.add(AnomalyType.HARD_BRAKING)
                    reasoning.add("Hard braking or acceleration detected with G-Force: %.2fG".format(motionSensor.gForce))
                } else {
                    classifications.add(AnomalyType.SEVERE_G_FORCE)
                    reasoning.add("Severe G-Force deviation detected: %.2fG".format(motionSensor.gForce))
                }
                maxConfidence = max(maxConfidence, max(gForceDeviation.confidence, 0.6f))
            }
        }

        // 2. Vehicle Telemetry Analysis
        if (vehicleTelemetry != null) {
            if (vehicleBaseline != null) {
                val speedDeviation = anomalyConfidenceEngine.calculateVehicleSpeedDeviation(
                    vehicleTelemetry.speedKmh,
                    vehicleBaseline
                )
                anomalyConfidenceEngine.calculateVehicleRpmDeviation(
                    vehicleTelemetry.rpm,
                    vehicleBaseline
                )

                if (speedDeviation.isOutsideNormalRange && speedDeviation.confidence >= 0.5f) {
                    abnormalSignalCount++
                    classifications.add(AnomalyType.RAPID_SPEED_DROP)
                    reasoning.add("Rapid speed change detected: %.1f km/h".format(vehicleTelemetry.speedKmh))
                    maxConfidence = max(maxConfidence, speedDeviation.confidence)
                }

                if (vehicleTelemetry.rpm == 0f && vehicleTelemetry.speedKmh > 20f) {
                    abnormalSignalCount++
                    classifications.add(AnomalyType.ENGINE_STOP)
                    reasoning.add("Engine stopped unexpectedly while moving at %.1f km/h".format(vehicleTelemetry.speedKmh))
                    maxConfidence = max(maxConfidence, 0.9f)
                }
            }

            if (!vehicleTelemetry.diagnosticFault.isNullOrBlank()) {
                abnormalSignalCount++
                classifications.add(AnomalyType.CRITICAL_VEHICLE_ANOMALY)
                reasoning.add("Diagnostic fault reported: ${vehicleTelemetry.diagnosticFault}")
                maxConfidence = max(maxConfidence, 0.85f)
            }
        }

        // 3. Driver State Analysis
        var driverAttentionFactor = 1.0f
        if (driverState != null) {
            when (driverState.condition) {
                DriverCondition.UNRESPONSIVE -> {
                    abnormalSignalCount++
                    classifications.add(AnomalyType.DRIVER_UNRESPONSIVE)
                    reasoning.add("Driver is unresponsive (Attention: %.2f)".format(driverState.attentionScore))
                    maxConfidence = max(maxConfidence, 0.95f)
                    driverAttentionFactor = 1.5f
                }
                DriverCondition.DROWSY -> {
                    abnormalSignalCount++
                    reasoning.add("Driver drowsiness detected (PERCLOS: %.2f)".format(driverState.perclos))
                    maxConfidence = max(maxConfidence, 0.7f)
                    driverAttentionFactor = 1.2f
                }
                DriverCondition.DISTRACTED -> {
                    reasoning.add("Driver distraction detected (Gaze away: %dms)".format(driverState.gazeAwayDurationMs))
                    maxConfidence = max(maxConfidence, 0.6f)
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
                reasoning.add("Night-time fatigue risk factor applied at hour $hour")
                maxConfidence = min(1.0f, maxConfidence * 1.15f)
            }

            // Stationary anomaly check: speed near 0 but engine RPM high or unexpected
            if (gpsLocation.speedMetersPerSecond < 0.5f && vehicleTelemetry != null && vehicleTelemetry.rpm > 3000f) {
                reasoning.add("Stationary high RPM anomaly detected")
                maxConfidence = max(maxConfidence, 0.65f)
            }
        }

        // 5. Recent History Compounding
        val recentWindowCount = recentHistory.count { now - it.timestamp < 30_000 } // last 30 seconds
        if (recentWindowCount > 0) {
            reasoning.add("Compounding factor from $recentWindowCount recent anomalies in last 30s")
            maxConfidence = min(1.0f, maxConfidence + (recentWindowCount * 0.1f))
        }

        // 6. Multiple Abnormal Signals Fusion
        if (abnormalSignalCount >= 2) {
            if (!classifications.contains(AnomalyType.MULTIPLE_ABNORMAL_SIGNALS)) {
                classifications.add(AnomalyType.MULTIPLE_ABNORMAL_SIGNALS)
            }
            reasoning.add("Multiple abnormal signals fused ($abnormalSignalCount signals detected)")
            maxConfidence = min(1.0f, maxConfidence * 1.2f)
        }

        val finalConfidence = (maxConfidence * driverAttentionFactor).coerceIn(0f, 1f)

        // Determine Severity
        val severity = when {
            finalConfidence >= 0.85f || classifications.contains(AnomalyType.DRIVER_UNRESPONSIVE) || classifications.contains(AnomalyType.ENGINE_STOP) -> AnomalySeverity.CRITICAL
            finalConfidence >= 0.7f || classifications.contains(AnomalyType.HARD_BRAKING) || classifications.contains(AnomalyType.MULTIPLE_ABNORMAL_SIGNALS) -> AnomalySeverity.HIGH
            finalConfidence >= 0.5f -> AnomalySeverity.MEDIUM
            else -> AnomalySeverity.LOW
        }

        // Explicit check for standard hard braking with alert driver -> requiresVerification = false
        val isAlertDriver = driverState == null || driverState.condition == DriverCondition.ALERT || driverState.isResponsive
        val isStandardBrakingOnly = classifications.size == 1 && classifications.contains(AnomalyType.HARD_BRAKING) && motionSensor != null && motionSensor.gForce <= 0.85f

        val requiresVerification = when {
            isStandardBrakingOnly && isAlertDriver -> false
            severity == AnomalySeverity.LOW -> false
            else -> true
        }

        val isEscalated = severity == AnomalySeverity.CRITICAL && finalConfidence >= 0.9f

        if (classifications.isEmpty()) {
            classifications.add(AnomalyType.UNKNOWN)
            reasoning.add("Normal driving operation observed within expected baseline parameters")
        }

        return AnomalyDetectionResult(
            confidence = finalConfidence,
            severity = severity,
            classifications = classifications.distinct(),
            requiresVerification = requiresVerification,
            isEscalated = isEscalated,
            reasoning = reasoning
        )
    }
}
