package com.example.okdrivers.ui.sensor_test

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.okdrivers.R
import com.example.okdrivers.sensors.test.SensorTestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SensorTestFragment :
    Fragment(R.layout.fragment_sensor_test) {

    private val viewModel: SensorTestViewModel by viewModels()

    private lateinit var tvStatus: TextView

    private lateinit var tvAccelerationX: TextView
    private lateinit var tvAccelerationY: TextView
    private lateinit var tvAccelerationZ: TextView

    private lateinit var tvGForce: TextView

    private lateinit var tvGyroX: TextView
    private lateinit var tvGyroY: TextView
    private lateinit var tvGyroZ: TextView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tvSensorStatus)

        tvAccelerationX =
            view.findViewById(R.id.tvAccelerationX)

        tvAccelerationY =
            view.findViewById(R.id.tvAccelerationY)

        tvAccelerationZ =
            view.findViewById(R.id.tvAccelerationZ)

        tvGForce =
            view.findViewById(R.id.tvGForce)

        tvGyroX =
            view.findViewById(R.id.tvGyroX)

        tvGyroY =
            view.findViewById(R.id.tvGyroY)

        tvGyroZ =
            view.findViewById(R.id.tvGyroZ)

        observeState()

        viewModel.startSensors()
    }

    private fun observeState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    tvStatus.text =
                        when {
                            state.error != null ->
                                "Error: ${state.error}"

                            state.isRunning ->
                                "Sensors running"

                            else ->
                                "Sensors stopped"
                        }

                    tvAccelerationX.text =
                        "X: %.3f m/s²"
                            .format(state.accelerationX)

                    tvAccelerationY.text =
                        "Y: %.3f m/s²"
                            .format(state.accelerationY)

                    tvAccelerationZ.text =
                        "Z: %.3f m/s²"
                            .format(state.accelerationZ)

                    tvGForce.text =
                        "G-force: %.3f G"
                            .format(state.gForce)

                    tvGyroX.text =
                        "X: %.4f rad/s"
                            .format(state.gyroX)

                    tvGyroY.text =
                        "Y: %.4f rad/s"
                            .format(state.gyroY)

                    tvGyroZ.text =
                        "Z: %.4f rad/s"
                            .format(state.gyroZ)
                }
            }
        }
    }

    override fun onDestroyView() {
        viewModel.stopSensors()
        super.onDestroyView()
    }
}