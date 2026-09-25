package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.SensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class AdaptiveBaselineEngine @Inject constructor() {

    companion object {
        /*
         * Higher alpha = adapt faster to recent driving.
         * Lower alpha = preserve historical behaviour longer.
         */
        private const val ALPHA = 0.10f

        private const val MIN_STD_DEV = 0.05f
    }

    fun updateDriverBaseline(
        currentSample: SensorSample,
        previousBaseline: BaselineStatistics?,
        now: Long = System.currentTimeMillis(),
        allowUpdate: Boolean = true
    ): BaselineUpdateResult {

        if (!allowUpdate) {
            return BaselineUpdateResult(
                baseline = previousBaseline
                    ?: createInitialDriverBaseline(
                        currentSample,
                        now
                    ),
                updated = false,
                updateTimestamp = now
            )
        }

        val baseline =
            if (previousBaseline == null) {
                createInitialDriverBaseline(
                    currentSample,
                    now
                )
            } else {
                updateDriverBaselineWithEma(
                    currentSample,
                    previousBaseline,
                    now
                )
            }

        return BaselineUpdateResult(
            baseline = baseline,
            updated = true,
            updateTimestamp = now
        )
    }

    fun updateVehicleBaseline(
        currentSample: VehicleTelemetrySample,
        previousBaseline: BaselineStatistics?,
        now: Long = System.currentTimeMillis(),
        allowUpdate: Boolean = true
    ): BaselineUpdateResult {

        if (!allowUpdate) {
            return BaselineUpdateResult(
                baseline = previousBaseline
                    ?: createInitialVehicleBaseline(
                        currentSample,
                        now
                    ),
                updated = false,
                updateTimestamp = now
            )
        }

        val baseline =
            if (previousBaseline == null) {
                createInitialVehicleBaseline(
                    currentSample,
                    now
                )
            } else {
                updateVehicleBaselineWithEma(
                    currentSample,
                    previousBaseline,
                    now
                )
            }

        return BaselineUpdateResult(
            baseline = baseline,
            updated = true,
            updateTimestamp = now
        )
    }

    private fun updateDriverBaselineWithEma(
        sample: SensorSample,
        previous: BaselineStatistics,
        now: Long
    ): BaselineStatistics {

        val speed =
            ema(
                previous.averageSpeedKmh,
                sample.speedKmh,
                ALPHA
            )

        val gForce =
            ema(
                previous.averageGForce,
                sample.gForce,
                ALPHA
            )

        val newSpeedStdDev =
            updateStdDev(
                previous.averageSpeedKmh,
                previous.speedStdDev,
                sample.speedKmh
            )

        val newGForceStdDev =
            updateStdDev(
                previous.averageGForce,
                previous.gForceStdDev,
                sample.gForce
            )

        return previous.copy(
            sampleCount = previous.sampleCount + 1,

            averageSpeedKmh = speed,
            minimumSpeedKmh =
                min(
                    previous.minimumSpeedKmh,
                    sample.speedKmh
                ),
            maximumSpeedKmh =
                max(
                    previous.maximumSpeedKmh,
                    sample.speedKmh
                ),
            speedStdDev = newSpeedStdDev,

            averageGForce = gForce,
            minimumGForce =
                min(
                    previous.minimumGForce,
                    sample.gForce
                ),
            maximumGForce =
                max(
                    previous.maximumGForce,
                    sample.gForce
                ),
            gForceStdDev = newGForceStdDev,

            updatedAt = now
        )
    }

    private fun updateVehicleBaselineWithEma(
        sample: VehicleTelemetrySample,
        previous: BaselineStatistics,
        now: Long
    ): BaselineStatistics {

        val speed =
            ema(
                previous.averageSpeedKmh,
                sample.speedKmh,
                ALPHA
            )

        val rpm =
            ema(
                previous.averageRpm,
                sample.rpm,
                ALPHA
            )

        val engineLoad =
            ema(
                previous.averageEngineLoad,
                sample.engineLoad,
                ALPHA
            )

        val newSpeedStdDev =
            updateStdDev(
                previous.averageSpeedKmh,
                previous.speedStdDev,
                sample.speedKmh
            )

        val newRpmStdDev =
            updateStdDev(
                previous.averageRpm,
                previous.rpmStdDev,
                sample.rpm
            )

        val newEngineLoadStdDev =
            updateStdDev(
                previous.averageEngineLoad,
                previous.engineLoadStdDev,
                sample.engineLoad
            )

        return previous.copy(
            sampleCount = previous.sampleCount + 1,

            averageSpeedKmh = speed,
            minimumSpeedKmh =
                min(
                    previous.minimumSpeedKmh,
                    sample.speedKmh
                ),
            maximumSpeedKmh =
                max(
                    previous.maximumSpeedKmh,
                    sample.speedKmh
                ),
            speedStdDev = newSpeedStdDev,

            averageRpm = rpm,
            minimumRpm =
                min(
                    previous.minimumRpm,
                    sample.rpm
                ),
            maximumRpm =
                max(
                    previous.maximumRpm,
                    sample.rpm
                ),
            rpmStdDev = newRpmStdDev,

            averageEngineLoad = engineLoad,
            minimumEngineLoad =
                min(
                    previous.minimumEngineLoad,
                    sample.engineLoad
                ),
            maximumEngineLoad =
                max(
                    previous.maximumEngineLoad,
                    sample.engineLoad
                ),
            engineLoadStdDev = newEngineLoadStdDev,

            updatedAt = now
        )
    }

    private fun ema(
        previous: Float,
        current: Float,
        alpha: Float
    ): Float {
        return previous +
                alpha * (current - previous)
    }

    private fun updateStdDev(
        previousMean: Float,
        previousStdDev: Float,
        currentValue: Float
    ): Float {

        val deviation =
            kotlin.math.abs(
                currentValue - previousMean
            )

        return max(
            MIN_STD_DEV,
            previousStdDev +
                    ALPHA * (deviation - previousStdDev)
        )
    }

    private fun createInitialDriverBaseline(
        sample: SensorSample,
        now: Long
    ): BaselineStatistics {

        return BaselineStatistics(
            sampleCount = 1,
            averageSpeedKmh = sample.speedKmh,
            minimumSpeedKmh = sample.speedKmh,
            maximumSpeedKmh = sample.speedKmh,
            speedStdDev = MIN_STD_DEV,
            averageGForce = sample.gForce,
            minimumGForce = sample.gForce,
            maximumGForce = sample.gForce,
            gForceStdDev = MIN_STD_DEV,
            averageRpm = 0f,
            minimumRpm = 0f,
            maximumRpm = 0f,
            rpmStdDev = 0f,
            averageEngineLoad = 0f,
            minimumEngineLoad = 0f,
            maximumEngineLoad = 0f,
            engineLoadStdDev = 0f,
            updatedAt = now
        )
    }

    private fun createInitialVehicleBaseline(
        sample: VehicleTelemetrySample,
        now: Long
    ): BaselineStatistics {

        return BaselineStatistics(
            sampleCount = 1,
            averageSpeedKmh = sample.speedKmh,
            minimumSpeedKmh = sample.speedKmh,
            maximumSpeedKmh = sample.speedKmh,
            speedStdDev = MIN_STD_DEV,
            averageGForce = 0f,
            minimumGForce = 0f,
            maximumGForce = 0f,
            gForceStdDev = 0f,
            averageRpm = sample.rpm,
            minimumRpm = sample.rpm,
            maximumRpm = sample.rpm,
            rpmStdDev = MIN_STD_DEV,
            averageEngineLoad = sample.engineLoad,
            minimumEngineLoad = sample.engineLoad,
            maximumEngineLoad = sample.engineLoad,
            engineLoadStdDev = MIN_STD_DEV,
            updatedAt = now
        )
    }
}