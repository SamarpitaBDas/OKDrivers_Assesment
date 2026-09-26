package com.example.okdrivers.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
open class MotionSensorManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val accelerometer by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    private val gyroscope by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    @Volatile
    private var simulatedGForce: Float? = null

    fun injectSimulatedGForce(value: Float) {
        simulatedGForce = value
    }

    fun clearSimulatedGForce() {
        simulatedGForce = null
    }

    open fun observeMotion(
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
                val calculatedGForce = accelerationMagnitude / SensorManager.GRAVITY_EARTH
                val finalGForce = simulatedGForce ?: calculatedGForce

                trySend(
                    MotionSensorSample(
                        timestamp = System.currentTimeMillis(),
                        accelerationX = latestAccelerationX,
                        accelerationY = latestAccelerationY,
                        accelerationZ = latestAccelerationZ,
                        gForce = finalGForce,
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

        if (registeredSensorCount == 0 && simulatedGForce == null) {
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
