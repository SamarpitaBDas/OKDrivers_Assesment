package com.example.okdrivers.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.okdrivers.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LiveDrivingFragment : Fragment(R.layout.fragment_live_driving) {

    private val viewModel: LiveDrivingViewModel by viewModels()

    private var tvTelemetryPrimary: TextView? = null
    private var tvTelemetrySecondary: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTelemetryPrimary = view.findViewById(R.id.tvTelemetryPrimary)
        tvTelemetrySecondary = view.findViewById(R.id.tvTelemetrySecondary)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    val speed = if (state.speedKmh != null) String.format(Locale.US, "%.1f km/h", state.speedKmh) else "-- km/h"
                    val rpm = if (state.rpm != null) "RPM ${state.rpm}" else "RPM ----"
                    tvTelemetryPrimary?.text = "$speed                         $rpm"

                    val gForce = if (state.gForce != null) String.format(Locale.US, "%.2f G", state.gForce) else "-- G"
                    val heading = if (state.heading != null) String.format(Locale.US, "%.0f°", state.heading) else "--°"
                    val loc = if (state.latitude != null && state.longitude != null) {
                        String.format(Locale.US, "Lat: %.4f°, Long: %.4f°", state.latitude, state.longitude)
                    } else {
                        "Acquiring GPS fix..."
                    }

                    tvTelemetrySecondary?.text = "G-Force: $gForce       Heading: $heading\nLocation: $loc"
                }
            }
        }
    }
}
