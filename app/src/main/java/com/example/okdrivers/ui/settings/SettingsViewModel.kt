package com.example.okdrivers.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.SamplingRate
import com.example.okdrivers.data.repository.SamplingRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val samplingRateRepository: SamplingRateRepository
) : ViewModel() {

    val currentRate: StateFlow<SamplingRate> = samplingRateRepository.samplingRateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SamplingRate.NORMAL
        )

    fun setSamplingRate(rate: SamplingRate) {
        viewModelScope.launch {
            samplingRateRepository.setSamplingRate(rate)
        }
    }
}
