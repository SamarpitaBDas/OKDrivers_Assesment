package com.example.okdrivers.domain.engine

import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample

interface BaselineEngine {

    fun calculateDriverBaseline(
        samples: List<MotionSensorSample>,
        now: Long = System.currentTimeMillis()
    ): BaselineStatistics

    fun calculateVehicleBaseline(
        samples: List<VehicleTelemetrySample>,
        now: Long = System.currentTimeMillis()
    ): BaselineStatistics
}