package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import org.junit.Assert.*
import org.junit.Test

class AnomalyEngineTest {

    private val anomalyConfidenceEngine = AnomalyConfidenceEngine()
    private val anomalyEngine = AnomalyEngine(anomalyConfidenceEngine)

    private val sampleDriverBaseline = BaselineStatistics(
        sampleCount = 50,
        averageSpeedKmh = 50f,
        minimumSpeedKmh = 0f,
        maximumSpeedKmh = 100f,
        speedStdDev = 5f,
        averageGForce = 1.0f,
        minimumGForce = 0.9f,
        maximumGForce = 1.2f,
        gForceStdDev = 0.05f,
        averageRpm = 2000f,
        minimumRpm = 800f,
        maximumRpm = 4000f,
        rpmStdDev = 200f,
        averageEngineLoad = 30f,
        minimumEngineLoad = 10f,
        maximumEngineLoad = 80f,
        engineLoadStdDev = 5f,
        updatedAt = System.currentTimeMillis()
    )

    private val sampleVehicleBaseline = sampleDriverBaseline

    @Test
    fun testNormalDriving() {
        val motion = MotionSensorSample(
            timestamp = System.currentTimeMillis(),
            accelerationX = 0f,
            accelerationY = 0f,
            accelerationZ = 9.8f,
            gForce = 1.0f,
            gyroX = 0f,
            gyroY = 0f,
            gyroZ = 0f
        )
        val telemetry = VehicleTelemetrySample(
            timestamp = System.currentTimeMillis(),
            speedKmh = 50f,
            rpm = 2000f,
            engineLoad = 30f,
            throttlePosition = 20f,
            engineTemperatureCelsius = 90f,
            batteryVoltage = 14.1f,
            diagnosticFault = null,
            airbagDeployed = false
        )
        val driverState = DriverState(
            timestamp = System.currentTimeMillis(),
            attentionScore = 0.95f,
            perclos = 0.05f,
            blinkRate = 15f,
            yawnDetected = false,
            gazeAwayDurationMs = 0L,
            headPitch = 0f,
            headYaw = 0f,
            headRoll = 0f,
            isResponsive = true,
            condition = DriverCondition.ALERT,
            gazeDirection = "CENTER"
        )
        val gps = GpsLocationSample(
            timestamp = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            speedMetersPerSecond = 13.88f,
            headingDegrees = 90f
        )

        val result = anomalyEngine.evaluate(
            vehicleTelemetry = telemetry,
            driverState = driverState,
            motionSensor = motion,
            gpsLocation = gps,
            driverBaseline = sampleDriverBaseline,
            vehicleBaseline = sampleVehicleBaseline
        )

        assertEquals(AnomalySeverity.LOW, result.severity)
        assertTrue(result.classifications.contains(AnomalyType.UNKNOWN))
        assertFalse(result.requiresVerification)
        assertFalse(result.isEscalated)
    }

    @Test
    fun testHardBraking08GWithAlertDriver() {
        val motion = MotionSensorSample(
            timestamp = System.currentTimeMillis(),
            accelerationX = 0f,
            accelerationY = -7.8f,
            accelerationZ = 5.0f,
            gForce = 0.8f, // 0.8G braking
            gyroX = 0f,
            gyroY = 0f,
            gyroZ = 0f
        )
        val driverState = DriverState(
            timestamp = System.currentTimeMillis(),
            attentionScore = 0.9f,
            perclos = 0.05f,
            blinkRate = 15f,
            yawnDetected = false,
            gazeAwayDurationMs = 0L,
            headPitch = 0f,
            headYaw = 0f,
            headRoll = 0f,
            isResponsive = true,
            condition = DriverCondition.ALERT,
            gazeDirection = "CENTER"
        )

        val result = anomalyEngine.evaluate(
            vehicleTelemetry = null,
            driverState = driverState,
            motionSensor = motion,
            gpsLocation = null,
            driverBaseline = sampleDriverBaseline,
            vehicleBaseline = null
        )

        assertTrue(result.classifications.contains(AnomalyType.HARD_BRAKING))
        assertFalse(result.requiresVerification) // Must be false per requirements for alert driver 0.8G braking
        assertFalse(result.isEscalated)
        assertTrue(result.events.isNotEmpty())
        assertFalse(result.events.first { it.type == AnomalyType.HARD_BRAKING }.requiresVerification)
        assertFalse(result.events.first { it.type == AnomalyType.HARD_BRAKING }.isEscalated)
    }

    @Test
    fun testDriverUnresponsiveness() {
        val driverState = DriverState(
            timestamp = System.currentTimeMillis(),
            attentionScore = 0.1f,
            perclos = 0.8f,
            blinkRate = 5f,
            yawnDetected = true,
            gazeAwayDurationMs = 5000L,
            headPitch = 15f,
            headYaw = 0f,
            headRoll = 0f,
            isResponsive = false,
            condition = DriverCondition.UNRESPONSIVE,
            gazeDirection = "DOWN"
        )

        val result = anomalyEngine.evaluate(
            vehicleTelemetry = null,
            driverState = driverState,
            motionSensor = null,
            gpsLocation = null,
            driverBaseline = null,
            vehicleBaseline = null
        )

        assertTrue(result.classifications.contains(AnomalyType.DRIVER_UNRESPONSIVE))
        assertEquals(AnomalySeverity.CRITICAL, result.severity)
        assertTrue(result.requiresVerification)
        assertTrue(result.isEscalated)
    }

    @Test
    fun testEngineStopAnomaly() {
        val telemetry = VehicleTelemetrySample(
            timestamp = System.currentTimeMillis(),
            speedKmh = 60f, // moving fast
            rpm = 0f, // engine stopped!
            engineLoad = 0f,
            throttlePosition = 0f,
            engineTemperatureCelsius = 85f,
            batteryVoltage = 12.0f,
            diagnosticFault = null,
            airbagDeployed = false
        )

        val result = anomalyEngine.evaluate(
            vehicleTelemetry = telemetry,
            driverState = null,
            motionSensor = null,
            gpsLocation = null,
            driverBaseline = null,
            vehicleBaseline = sampleVehicleBaseline
        )

        assertTrue(result.classifications.contains(AnomalyType.ENGINE_STOP))
        assertEquals(AnomalySeverity.CRITICAL, result.severity)
        assertTrue(result.requiresVerification)
        assertTrue(result.isEscalated)
    }

    @Test
    fun testAirbagDeploymentAnomaly() {
        val telemetry = VehicleTelemetrySample(
            timestamp = System.currentTimeMillis(),
            speedKmh = 0f,
            rpm = 0f,
            engineLoad = 0f,
            throttlePosition = 0f,
            engineTemperatureCelsius = 90f,
            batteryVoltage = 12.6f,
            diagnosticFault = null,
            airbagDeployed = true // Airbag deployed
        )

        val result = anomalyEngine.evaluate(
            vehicleTelemetry = telemetry,
            driverState = null,
            motionSensor = null,
            gpsLocation = null,
            driverBaseline = null,
            vehicleBaseline = null
        )

        assertTrue(result.classifications.contains(AnomalyType.CRITICAL_VEHICLE_ANOMALY))
        assertEquals(AnomalySeverity.CRITICAL, result.severity)
        assertTrue(result.requiresVerification)
    }

    @Test
    fun testMultipleAbnormalSignals() {
        val motion = MotionSensorSample(
            timestamp = System.currentTimeMillis(),
            accelerationX = 0f,
            accelerationY = -12f,
            accelerationZ = 2.0f,
            gForce = 0.5f, // Hard braking (0.5G)
            gyroX = 0f,
            gyroY = 0f,
            gyroZ = 0f
        )
        val driverState = DriverState(
            timestamp = System.currentTimeMillis(),
            attentionScore = 0.1f,
            perclos = 0.8f,
            blinkRate = 25f,
            yawnDetected = true,
            gazeAwayDurationMs = 5000L,
            headPitch = 0f,
            headYaw = 0f,
            headRoll = 0f,
            isResponsive = false,
            condition = DriverCondition.UNRESPONSIVE,
            gazeDirection = "DOWN"
        )

        val result = anomalyEngine.evaluate(
            vehicleTelemetry = null,
            driverState = driverState,
            motionSensor = motion,
            gpsLocation = null,
            driverBaseline = sampleDriverBaseline,
            vehicleBaseline = null
        )

        assertTrue(result.classifications.contains(AnomalyType.MULTIPLE_ABNORMAL_SIGNALS))
        assertTrue(result.classifications.contains(AnomalyType.HARD_BRAKING))
        assertTrue(result.classifications.contains(AnomalyType.DRIVER_UNRESPONSIVE))
        assertTrue(result.severity == AnomalySeverity.HIGH || result.severity == AnomalySeverity.CRITICAL)
        assertTrue(result.requiresVerification)
    }
}
