package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.AnomalyRepository
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import javax.inject.Inject

class AnomalyManager @Inject constructor(
    private val anomalyEngine: AnomalyEngine,
    private val anomalyRepository: AnomalyRepository,
    private val incidentStateMachine: IncidentStateMachine
) {

    suspend fun evaluateAndPersist(
        driverId: String,
        vehicleId: String,
        vehicleTelemetry: VehicleTelemetrySample?,
        driverState: DriverState?,
        motionSensor: MotionSensorSample?,
        gpsLocation: GpsLocationSample?,
        driverBaseline: BaselineStatistics?,
        vehicleBaseline: BaselineStatistics?,
        recentHistory: List<AnomalyEvent> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): AnomalyDetectionResult {

        val result = anomalyEngine.evaluate(
            driverId = driverId,
            vehicleId = vehicleId,
            vehicleTelemetry = vehicleTelemetry,
            driverState = driverState,
            motionSensor = motionSensor,
            gpsLocation = gpsLocation,
            driverBaseline = driverBaseline,
            vehicleBaseline = vehicleBaseline,
            recentHistory = recentHistory,
            now = now
        )

        // Persist every AnomalyEvent regardless of whether it escalates, ensuring a full log
        for (event in result.events) {
            anomalyRepository.saveAnomaly(event)

            // Feed rule trigger reason + severity into the incident state machine
            incidentStateMachine.handleAnomalyEvent(
                event = event,
                driverId = driverId,
                vehicleId = vehicleId,
                now = now
            )
        }

        return result
    }
}
