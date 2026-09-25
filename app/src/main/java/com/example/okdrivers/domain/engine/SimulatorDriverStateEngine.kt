package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.DmsSample
import javax.inject.Inject

class SimulatorDriverStateEngine @Inject constructor() :
    DriverStateEngine {

    override fun process(sample: DmsSample): DriverState {

        return DriverState(
            timestamp = sample.timestamp,
            attentionScore = sample.attentionScore,
            perclos = sample.perclos,
            blinkRate = sample.blinkRate,
            yawnDetected = sample.yawnDetected,
            gazeAwayDurationMs = sample.gazeAwayDurationMs,
            headPitch = sample.headPitch,
            headYaw = sample.headYaw,
            headRoll = sample.headRoll,
            gazeDirection = sample.gazeDirection.name,
            isResponsive = sample.isResponsive,
            condition = sample.condition
        )
    }
}