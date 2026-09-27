package com.example.okdrivers.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SensorDataFragment : Fragment(R.layout.fragment_sensor_data) {

    private val viewModel: SensorDataViewModel by viewModels()
    private var tvSensorContent: TextView? = null
    private var btnBack: MaterialButton? = null
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvSensorContent = view.findViewById(R.id.tvSensorContent)
        btnBack = view.findViewById(R.id.btnBack)

        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    val text = """
                        ✣  Accelerometer
                            X: ${String.format(Locale.US, "%.2f", state.accelX)}   Y: ${String.format(Locale.US, "%.2f", state.accelY)}   Z: ${String.format(Locale.US, "%.2f", state.accelZ)} G (Total: ${String.format(Locale.US, "%.2f", state.gForce)}G)

                        ◉  Gyroscope
                            Pitch: ${String.format(Locale.US, "%.1f°", state.gyroX)}   Roll: ${String.format(Locale.US, "%.1f°", state.gyroY)}   Yaw: ${String.format(Locale.US, "%.1f°", state.gyroZ)}

                        📍  GPS
                            Lat: ${String.format(Locale.US, "%.4f°", state.latitude)}   Long: ${String.format(Locale.US, "%.4f°", state.longitude)}
                            Speed: ${String.format(Locale.US, "%.1f km/h", state.speedKmh)}   Heading: ${String.format(Locale.US, "%.0f°", state.heading)}

                        ▣  Battery
                            ${state.batteryPercentage}%   Charging: ${if (state.isCharging) "Yes" else "No"}

                        ⌁  Network
                            ${if (state.isOnline) "Online" else "Offline"}

                        ▣  Vehicle Telemetry
                            Speed: ${String.format(Locale.US, "%.1f km/h", state.telemetrySpeed)}   RPM: ${state.telemetryRpm.toInt()}
                            Engine Load: ${state.engineLoad.toInt()}%  Temp: ${String.format(Locale.US, "%.1f°C", state.engineTemp)}

                        ◷  Timestamp
                            ${timeFormat.format(Date(state.timestamp))}
                    """.trimIndent()

                    tvSensorContent?.text = text
                }
            }
        }
    }
}
