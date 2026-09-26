package com.example.okdrivers.sensors

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleTelemetrySimulator @Inject constructor() {

    enum class AnomalyType {
        NONE,
        HARD_BRAKING,
        ENGINE_STOP,
        HIGH_TEMPERATURE,
        LOW_VOLTAGE,
        DIAGNOSTIC_FAULT,
        AIRBAG_DEPLOYED
    }

    @Volatile
    private var activeAnomaly = AnomalyType.NONE

    fun injectAnomaly(type: AnomalyType) {
        activeAnomaly = type
    }

    fun clearAnomaly() {
        activeAnomaly = AnomalyType.NONE
    }

    fun observeTelemetry(
        intervalMillis: Long = 1000L
    ): Flow<VehicleTelemetrySample> = flow {

        var speed = 45f
        var rpm = 1800f
        var engineLoad = 35f
        var throttle = 25f
        var temperature = 88f
        var voltage = 13.9f
        var airbag = false

        while (true) {

            /*
             * Normal vehicle drift.
             */
            speed += Random.nextFloat() * 4f - 2f
            rpm += Random.nextFloat() * 160f - 80f
            engineLoad += Random.nextFloat() * 6f - 3f
            throttle += Random.nextFloat() * 5f - 2.5f
            temperature += Random.nextFloat() * 1.5f - 0.75f
            voltage += Random.nextFloat() * 0.08f - 0.04f
            airbag = false

            /*
             * Keep normal values realistic.
             */
            speed = speed.coerceIn(0f, 120f)
            rpm = rpm.coerceIn(700f, 5000f)
            engineLoad = engineLoad.coerceIn(5f, 100f)
            throttle = throttle.coerceIn(0f, 100f)
            temperature = temperature.coerceIn(70f, 110f)
            voltage = voltage.coerceIn(11.5f, 14.8f)

            var diagnosticFault: String? = null

            /*
             * Apply requested anomaly.
             */
            when (activeAnomaly) {

                AnomalyType.HARD_BRAKING -> {
                    speed = (speed - 35f).coerceAtLeast(0f)
                    throttle = 0f
                    engineLoad = 10f
                }

                AnomalyType.ENGINE_STOP -> {
                    speed = (speed - 5f).coerceAtLeast(0f)
                    rpm = 0f
                    throttle = 0f
                    engineLoad = 0f
                }

                AnomalyType.HIGH_TEMPERATURE -> {
                    temperature = 125f
                }

                AnomalyType.LOW_VOLTAGE -> {
                    voltage = 10.5f
                }

                AnomalyType.DIAGNOSTIC_FAULT -> {
                    diagnosticFault = "ENGINE_FAULT"
                }

                AnomalyType.AIRBAG_DEPLOYED -> {
                    airbag = true
                    speed = 0f
                    rpm = 0f
                }

                AnomalyType.NONE -> {
                    // Normal operation.
                }
            }

            emit(
                VehicleTelemetrySample(
                    timestamp = System.currentTimeMillis(),
                    speedKmh = speed,
                    rpm = rpm,
                    engineLoad = engineLoad,
                    throttlePosition = throttle,
                    engineTemperatureCelsius = temperature,
                    batteryVoltage = voltage,
                    diagnosticFault = diagnosticFault,
                    airbagDeployed = airbag
                )
            )

            delay(intervalMillis)
        }
    }
}
