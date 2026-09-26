package com.example.okdrivers.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.example.okdrivers.data.repository.SamplingRate
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        switchBatterySaver = view.findViewById(R.id.switchBatterySaver)
        cardProfile = view.findViewById(R.id.cardProfile)
        cardProfileVehicle = view.findViewById(R.id.cardProfileVehicle)

        cardProfile?.setOnClickListener {
            findNavController().navigate(R.id.driverProfileFragment)
        }

        cardProfileVehicle?.setOnClickListener {
            findNavController().navigate(R.id.vehicleProfileFragment)
        }

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
}
