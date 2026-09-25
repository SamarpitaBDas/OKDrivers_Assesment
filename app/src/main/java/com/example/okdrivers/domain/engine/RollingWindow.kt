package com.example.okdrivers.domain.engine

import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample

object RollingWindow {

    fun motion(
        samples: List<MotionSensorSample>,
        windowMillis: Long,
        now: Long = System.currentTimeMillis()
    ): List<MotionSensorSample> {

        val cutoff = now - windowMillis

        return samples.filter {
            it.timestamp >= cutoff
        }
    }

    fun telemetry(
        samples: List<VehicleTelemetrySample>,
        windowMillis: Long,
        now: Long = System.currentTimeMillis()
    ): List<VehicleTelemetrySample> {

        val cutoff = now - windowMillis

        return samples.filter {
            it.timestamp >= cutoff
        }
    }
}