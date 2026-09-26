package com.example.okdrivers.ui.analytics

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class BaselineAnalyticsFragment : Fragment(R.layout.fragment_baseline_analytics) {

    private val viewModel: BaselineAnalyticsViewModel by viewModels()

    private var btnBack: MaterialButton? = null
    private var pbConfidence: LinearProgressIndicator? = null
    private var tvConfidencePercentage: TextView? = null
    private var tvSampleCount: TextView? = null
    private var tvLearningStatus: TextView? = null
    private var tvDeviationBadge: TextView? = null
    private var tvGForceZScore: TextView? = null
    private var tvSpeedZScore: TextView? = null
    private var tvAverageSpeed: TextView? = null
    private var tvAverageRpm: TextView? = null
    private var tvAverageEngineLoad: TextView? = null
    private var tvAverageBrakingG: TextView? = null
    private var tvMaxNormalG: TextView? = null
    private var tvLastUpdated: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        pbConfidence = view.findViewById(R.id.pbConfidence)
        tvConfidencePercentage = view.findViewById(R.id.tvConfidencePercentage)
        tvSampleCount = view.findViewById(R.id.tvSampleCount)
        tvLearningStatus = view.findViewById(R.id.tvLearningStatus)
        tvDeviationBadge = view.findViewById(R.id.tvDeviationBadge)
        tvGForceZScore = view.findViewById(R.id.tvGForceZScore)
        tvSpeedZScore = view.findViewById(R.id.tvSpeedZScore)
        tvAverageSpeed = view.findViewById(R.id.tvAverageSpeed)
        tvAverageRpm = view.findViewById(R.id.tvAverageRpm)
        tvAverageEngineLoad = view.findViewById(R.id.tvAverageEngineLoad)
        tvAverageBrakingG = view.findViewById(R.id.tvAverageBrakingG)
        tvMaxNormalG = view.findViewById(R.id.tvMaxNormalG)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)

        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: BaselineAnalyticsUiState) {
        pbConfidence?.progress = state.confidencePercent.coerceIn(0, 100)
        tvConfidencePercentage?.text = "${state.confidencePercent}%"
        tvSampleCount?.text = "Samples Collected: ${state.sampleCount}"

        if (state.hasBaseline) {
            tvLearningStatus?.text = "Baseline Calibrated & Active"
            tvLearningStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
        } else {
            tvLearningStatus?.text = "Still learning driving patterns... (sample count < 10)"
            tvLearningStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_orange))
        }

        if (state.isCurrentlyOutsideNormalRange) {
            tvDeviationBadge?.text = "ELEVATED DEVIATION"
            tvDeviationBadge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_orange))
        } else {
            tvDeviationBadge?.text = "WITHIN NORMAL RANGE"
            tvDeviationBadge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
        }

        val gDev = state.currentGForceDeviation ?: 0f
        val speedDev = state.currentSpeedDeviation ?: 0f
        tvGForceZScore?.text = String.format(Locale.US, "G-Force Deviation: %.2f σ", gDev)
        tvSpeedZScore?.text = String.format(Locale.US, "Speed Deviation: %.2f σ", speedDev)

        tvAverageSpeed?.text = String.format(Locale.US, "Average Speed: %.1f km/h", state.averageSpeedKmh ?: 0f)
        tvAverageRpm?.text = "Average RPM: ${state.averageRpm ?: 0} RPM"
        tvAverageEngineLoad?.text = String.format(Locale.US, "Average Engine Load: %.1f%%", state.averageEngineLoad ?: 0f)
        tvAverageBrakingG?.text = String.format(Locale.US, "Average Braking G-Force: %.2f G", state.averageBrakingG ?: 0f)
        tvMaxNormalG?.text = String.format(Locale.US, "Max Normal G-Force: %.2f G", state.maximumNormalGForce ?: 0f)
        tvLastUpdated?.text = "Last Updated: ${state.lastUpdatedText}"
    }
}
