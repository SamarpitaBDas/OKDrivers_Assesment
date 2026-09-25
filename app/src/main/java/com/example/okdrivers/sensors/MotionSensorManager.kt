package com.example.okdrivers.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MotionSensorManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    fun observeMotion(
        samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME
    ): Flow<MotionSensorSample> = callbackFlow {
        var latestAccelerationX = 0f
        var latestAccelerationY = 0f
        var latestAccelerationZ = 0f

        var latestGyroX = 0f
        var latestGyroY = 0f
        var latestGyroZ = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        latestAccelerationX = event.values[0]
                        latestAccelerationY = event.values[1]
                        latestAccelerationZ = event.values[2]
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        latestGyroX = event.values[0]
                        latestGyroY = event.values[1]
                        latestGyroZ = event.values[2]
                    }
                }
                val accelerationMagnitude = sqrt(
                    latestAccelerationX * latestAccelerationX +
                            latestAccelerationY * latestAccelerationY +
                            latestAccelerationZ * latestAccelerationZ
                )
                val gForce =
                    accelerationMagnitude / SensorManager.GRAVITY_EARTH
                trySend(
                    MotionSensorSample(
                        timestamp = System.currentTimeMillis(),
                        accelerationX = latestAccelerationX,
                        accelerationY = latestAccelerationY,
                        accelerationZ = latestAccelerationZ,
                        gForce = gForce,
                        gyroX = latestGyroX,
                        gyroY = latestGyroY,
                        gyroZ = latestGyroZ
                    )
                )
            }
            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int
            ) {
                //currently no accuracy changes
            }
        }

        var registeredSensorCount = 0

        accelerometer?.let {
            if (
                sensorManager.registerListener(
                    listener,
                    it,
                    samplingPeriodUs
                )
            ) {
                registeredSensorCount++
            }
        }

        gyroscope?.let {
            if (
                sensorManager.registerListener(
                    listener,
                    it,
                    samplingPeriodUs
                )
            ) {
                registeredSensorCount++
            }
        }

        if (registeredSensorCount == 0) {
            close(
                IllegalStateException(
                    "Neither accelerometer nor gyroscope is available."
                )
            )
        }
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}