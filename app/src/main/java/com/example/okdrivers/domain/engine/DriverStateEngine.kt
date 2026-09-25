package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.DmsSample

interface DriverStateEngine {

    fun process(sample: DmsSample): DriverState
}