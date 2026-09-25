package com.example.okdrivers.data.repository

import com.example.okdrivers.sensors.BatteryStatusSample
import com.example.okdrivers.sensors.DmsSample
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.NetworkStatusSample
import com.example.okdrivers.sensors.SensorSnapshot
import com.example.okdrivers.sensors.VehicleTelemetrySample

import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun observeMotion(): Flow<MotionSensorSample>
    fun observeGps(): Flow<GpsLocationSample>
    fun observeBattery(): Flow<BatteryStatusSample>
    fun observeNetwork(): Flow<NetworkStatusSample>
    fun observeVehicleTelemetry(): Flow<VehicleTelemetrySample>
    fun observeDms(): Flow<DmsSample>
    fun observeSensorSnapshot(): Flow<SensorSnapshot>
}