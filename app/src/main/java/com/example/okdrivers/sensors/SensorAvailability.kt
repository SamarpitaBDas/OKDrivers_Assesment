package com.example.okdrivers.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

data class SensorAvailability(
    val accelerometerAvailable: Boolean,
    val gyroscopeAvailable: Boolean
)

fun getSensorAvailability(
    context: Context
): SensorAvailability {
    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return SensorAvailability(
        accelerometerAvailable =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            ) != null,
        gyroscopeAvailable =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            ) != null
    )
}