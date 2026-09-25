package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.SensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample

interface BaselineEngine {

    fun calculateDriverBaseline(
        samples: List<SensorSample>,
        now: Long = System.currentTimeMillis()
    ): BaselineStatistics

    fun calculateVehicleBaseline(
        samples: List<VehicleTelemetrySample>,
        now: Long = System.currentTimeMillis()
    ): BaselineStatistics
}