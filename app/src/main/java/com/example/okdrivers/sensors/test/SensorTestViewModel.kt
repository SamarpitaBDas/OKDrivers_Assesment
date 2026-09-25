package com.example.okdrivers.sensors.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.sensors.MotionSensorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SensorTestViewModel @Inject constructor(
    private val motionSensorManager: MotionSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SensorTestUiState()
    )

    val uiState: StateFlow<SensorTestUiState> =
        _uiState.asStateFlow()

    private var sensorJob: Job? = null

    fun startSensors() {

        if (sensorJob?.isActive == true) {
            return
        }

        sensorJob = viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isRunning = true,
                    error = null
                )
            }

            motionSensorManager
                .observeMotion()
                .catch { throwable ->

                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            error = throwable.message
                                ?: "Unable to read sensors"
                        )
                    }
                }
                .collect { sample ->

                    _uiState.update {
                        it.copy(
                            isRunning = true,

                            accelerationX =
                                sample.accelerationX,

                            accelerationY =
                                sample.accelerationY,

                            accelerationZ =
                                sample.accelerationZ,

                            gForce =
                                sample.gForce,

                            gyroX =
                                sample.gyroX,

                            gyroY =
                                sample.gyroY,

                            gyroZ =
                                sample.gyroZ,

                            lastUpdated =
                                sample.timestamp,

                            error = null
                        )
                    }
                }
        }
    }

    fun stopSensors() {

        sensorJob?.cancel()
        sensorJob = null

        _uiState.update {
            it.copy(
                isRunning = false
            )
        }
    }

    override fun onCleared() {
        stopSensors()
        super.onCleared()
    }
}