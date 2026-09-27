package com.example.okdrivers.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.example.okdrivers.data.repository.SamplingRate
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()
    private var switchBatterySaver: SwitchCompat? = null
    private var cardProfile: MaterialCardView? = null
    private var cardProfileVehicle: MaterialCardView? = null
    private var cardBaselines: MaterialCardView? = null
    private var cardLiveDriving: MaterialCardView? = null
    private var cardSensorData: MaterialCardView? = null
    private var cardTestControls: MaterialCardView? = null
    private var cardNotifications: MaterialCardView? = null
    private var cardVoiceAudio: MaterialCardView? = null
    private var cardPrivacy: MaterialCardView? = null
    private var cardAbout: MaterialCardView? = null
    private var btnLogout: MaterialButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        switchBatterySaver = view.findViewById(R.id.switchBatterySaver)
        cardProfile = view.findViewById(R.id.cardProfile)
        cardProfileVehicle = view.findViewById(R.id.cardProfileVehicle)
        cardBaselines = view.findViewById(R.id.cardBaselines)
        cardLiveDriving = view.findViewById(R.id.cardLiveDriving)
        cardSensorData = view.findViewById(R.id.cardSensorData)
        cardTestControls = view.findViewById(R.id.cardTestControls)
        cardNotifications = view.findViewById(R.id.cardNotifications)
        cardVoiceAudio = view.findViewById(R.id.cardVoiceAudio)
        cardPrivacy = view.findViewById(R.id.cardPrivacy)
        cardAbout = view.findViewById(R.id.cardAbout)
        btnLogout = view.findViewById(R.id.btnLogout)

        setupClickListeners()

        switchBatterySaver?.setOnCheckedChangeListener { _, isChecked ->
            val rate = if (isChecked) SamplingRate.BATTERY_SAVER else SamplingRate.NORMAL
            viewModel.setSamplingRate(rate)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentRate.collectLatest { rate ->
                    val isBatterySaver = rate == SamplingRate.BATTERY_SAVER
                    if (switchBatterySaver?.isChecked != isBatterySaver) {
                        switchBatterySaver?.isChecked = isBatterySaver
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        cardProfile?.setOnClickListener {
            findNavController().navigate(R.id.driverProfileFragment)
        }

        cardProfileVehicle?.setOnClickListener {
            findNavController().navigate(R.id.vehicleProfileFragment)
        }

        cardBaselines?.setOnClickListener {
            findNavController().navigate(R.id.baselineAnalyticsFragment)
        }

        cardLiveDriving?.setOnClickListener {
            findNavController().navigate(R.id.liveDrivingFragment)
        }

        cardSensorData?.setOnClickListener {
            findNavController().navigate(R.id.sensorDataFragment)
        }

        cardTestControls?.setOnClickListener {
            findNavController().navigate(R.id.testControlsFragment)
        }

        cardVoiceAudio?.setOnClickListener {
            findNavController().navigate(R.id.aiVerificationFragment)
        }

        cardNotifications?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Notification Settings")
                .setMessage("Emergency, Community Mobilization, and Safety Alerts are currently ACTIVE with HIGH priority.")
                .setPositiveButton("OK", null)
                .show()
        }

        cardPrivacy?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Privacy & Security")
                .setMessage("All driver telemetry and sensor snapshots are encrypted on-device. Location data is shared only during verified emergency incidents.")
                .setPositiveButton("OK", null)
                .show()
        }

        cardAbout?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About okDriver")
                .setMessage("okDriver v1.0\nIntelligent Real-time Driver Safety Monitoring & Community Emergency Response System")
                .setPositiveButton("OK", null)
                .show()
        }

        btnLogout?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out and reset the current session?")
                .setPositiveButton("Log Out") { _, _ ->
                    Toast.makeText(requireContext(), "Logged out — Session reset", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.homeFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
