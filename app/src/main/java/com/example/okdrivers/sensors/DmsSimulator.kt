package com.example.okdrivers.sensors

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import javax.inject.Inject
import com.example.okdrivers.domain.model.DriverCondition

class DmsSimulator @Inject constructor() {

    enum class AnomalyType {
        NONE,
        DROWSINESS,
        DISTRACTION,
        UNRESPONSIVE
    }

    @Volatile
    private var activeAnomaly = AnomalyType.NONE

    fun injectAnomaly(type: AnomalyType) {
        activeAnomaly = type
    }

    fun clearAnomaly() {
        activeAnomaly = AnomalyType.NONE
    }

    fun observeDms(
        intervalMillis: Long = 1000L
    ): Flow<DmsSample> = flow {

        var perclos = 0.08f
        var headPitch = 0f
        var headYaw = 0f
        var headRoll = 0f

        var blinkRate = 14f
        var gazeAwayDurationMs = 0L
        var attentionScore = 0.92f

        while (true) {

            /*
             * Normal driver behaviour drift.
             */
            perclos += Random.nextFloat() * 0.02f - 0.01f

            headPitch += Random.nextFloat() * 2f - 1f
            headYaw += Random.nextFloat() * 3f - 1.5f
            headRoll += Random.nextFloat() * 1.5f - 0.75f

            blinkRate += Random.nextFloat() * 2f - 1f

            attentionScore +=
                Random.nextFloat() * 0.04f - 0.02f

            perclos = perclos.coerceIn(0f, 1f)

            headPitch = headPitch.coerceIn(-30f, 30f)
            headYaw = headYaw.coerceIn(-45f, 45f)
            headRoll = headRoll.coerceIn(-20f, 20f)

            blinkRate = blinkRate.coerceIn(5f, 40f)

            attentionScore =
                attentionScore.coerceIn(0f, 1f)

            var gazeDirection = GazeDirection.FORWARD
            var yawnDetected = false
            var isResponsive = true
            var condition = DriverCondition.ALERT

            /*
             * Apply deterministic test anomaly.
             */
            when (activeAnomaly) {

                AnomalyType.NONE -> {
                    // Normal operation.
                }

                AnomalyType.DROWSINESS -> {

                    perclos = 0.35f

                    blinkRate = 6f

                    yawnDetected = true

                    attentionScore = 0.45f

                    condition = DriverCondition.DROWSY
                }

                AnomalyType.DISTRACTION -> {

                    gazeDirection =
                        if (Random.nextBoolean()) {
                            GazeDirection.LEFT
                        } else {
                            GazeDirection.RIGHT
                        }

                    gazeAwayDurationMs += 1000L

                    headYaw =
                        if (gazeDirection == GazeDirection.LEFT) {
                            -35f
                        } else {
                            35f
                        }

                    attentionScore = 0.40f

                    condition = DriverCondition.DISTRACTED
                }

                AnomalyType.UNRESPONSIVE -> {

                    perclos = 0.45f

                    attentionScore = 0.15f

                    isResponsive = false

                    gazeDirection = GazeDirection.FORWARD

                    condition = DriverCondition.UNRESPONSIVE
                }
            }

            /*
             * Prevent gaze-away timer from growing
             * during normal operation.
             */
            if (activeAnomaly != AnomalyType.DISTRACTION) {
                gazeAwayDurationMs = 0L
            }

            emit(
                DmsSample(
                    timestamp = System.currentTimeMillis(),

                    perclos = perclos,

                    gazeDirection = gazeDirection,

                    headPitch = headPitch,
                    headYaw = headYaw,
                    headRoll = headRoll,

                    blinkRate = blinkRate,
                    yawnDetected = yawnDetected,

                    gazeAwayDurationMs =
                        gazeAwayDurationMs,

                    attentionScore =
                        attentionScore,

                    isResponsive =
                        isResponsive,

                    condition =
                        condition
                )
            )

            delay(intervalMillis)
        }
    }
}