package com.example.okdrivers.data.repository

import com.example.okdrivers.sensors.BatteryStatusManager
import com.example.okdrivers.sensors.BatteryStatusSample
import com.example.okdrivers.sensors.DmsSample
import com.example.okdrivers.sensors.DmsSimulator
import com.example.okdrivers.sensors.GpsLocationManager
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorManager
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.NetworkStatusManager
import com.example.okdrivers.sensors.NetworkStatusSample
import com.example.okdrivers.sensors.SensorSnapshot
import com.example.okdrivers.sensors.VehicleTelemetrySample
import com.example.okdrivers.sensors.VehicleTelemetrySimulator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val motionSensorManager: MotionSensorManager,
    private val gpsLocationManager: GpsLocationManager,
    private val batteryStatusManager: BatteryStatusManager,
    private val networkStatusManager: NetworkStatusManager,
    private val vehicleTelemetrySimulator: VehicleTelemetrySimulator,
    private val dmsSimulator: DmsSimulator
) : SensorRepository {

    override fun observeMotion():
            Flow<MotionSensorSample> {

        return motionSensorManager
            .observeMotion()
    }

    override fun observeGps():
            Flow<GpsLocationSample> {

        return gpsLocationManager
            .observeLocation()
    }

    override fun observeBattery():
            Flow<BatteryStatusSample> {

        return batteryStatusManager
            .observeBattery()
    }

    override fun observeNetwork():
            Flow<NetworkStatusSample> {

        return networkStatusManager
            .observeNetwork()
    }

    override fun observeVehicleTelemetry():
            Flow<VehicleTelemetrySample> {

        return vehicleTelemetrySimulator
            .observeTelemetry()
    }

    override fun observeDms():
            Flow<DmsSample> {

        return dmsSimulator
            .observeDms()
    }

    override fun observeSensorSnapshot(): Flow<SensorSnapshot> {

        return combine(
            observeMotion(),
            observeGps(),
            observeBattery(),
            observeNetwork(),
            observeVehicleTelemetry(),
            observeDms()
        ) { values ->

            @Suppress("UNCHECKED_CAST")
            val motion = values[0] as MotionSensorSample

            @Suppress("UNCHECKED_CAST")
            val gps = values[1] as GpsLocationSample

            @Suppress("UNCHECKED_CAST")
            val battery = values[2] as BatteryStatusSample

            @Suppress("UNCHECKED_CAST")
            val network = values[3] as NetworkStatusSample

            @Suppress("UNCHECKED_CAST")
            val vehicleTelemetry =
                values[4] as VehicleTelemetrySample

            @Suppress("UNCHECKED_CAST")
            val dms = values[5] as DmsSample

            SensorSnapshot(
                motion = motion,
                gps = gps,
                battery = battery,
                network = network,
                vehicleTelemetry = vehicleTelemetry,
                dms = dms
            )
        }
    }
}