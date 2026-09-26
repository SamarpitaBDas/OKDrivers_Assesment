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
class DriverStateFragment : Fragment(R.layout.fragment_driver_state) {

    private val viewModel: DriverStateViewModel by viewModels()
    private var tvDriverStateContent: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvDriverStateContent = view.findViewById(R.id.tvDriverStateContent)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    val text = """
                        PERCLOS        ${String.format(Locale.US, "%.1f%%", state.perclos * 100f)}

                        Gaze Direction        ${state.gazeDirection}

                        Head Pose        Pitch: ${String.format(Locale.US, "%.1f°", state.headPitch)}, Yaw: ${String.format(Locale.US, "%.1f°", state.headYaw)}

                        Blink Rate / Yawn        ${String.format(Locale.US, "%.1f bpm", state.blinkRate)} / ${if (state.yawnDetected) "Yes" else "No"}

                        Attention State        ${state.condition.name} (Score: ${String.format(Locale.US, "%.2f", state.attentionScore)})

                        Responsiveness        ${if (state.isResponsive) "Responsive" else "Unresponsive"}
                    """.trimIndent()

                    tvDriverStateContent?.text = text
                }
            }
        }
    }
}
