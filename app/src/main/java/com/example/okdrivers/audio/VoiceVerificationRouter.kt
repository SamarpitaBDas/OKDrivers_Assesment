package com.example.okdrivers.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.okdrivers.domain.model.UrgencyLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class VoiceVerificationRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val realService: RealVoiceVerificationService,
    private val simulatedService: SimulatedVoiceVerificationService
) : VoiceVerificationService, DemoVoiceResponseController {

    private var forceSimulatedMode: Boolean = false

    override fun setNextSimulatedResponse(transcript: String?) {
        simulatedService.simulatedResponse = transcript
    }

    override fun setForceSimulatedMode(enabled: Boolean) {
        forceSimulatedMode = enabled
    }

    override fun isForceSimulatedMode(): Boolean {
        return forceSimulatedMode
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    open fun shouldUseRealService(): Boolean {
        if (forceSimulatedMode) {
            return false
        }
        return hasRecordAudioPermission() &&
                isNetworkAvailable() &&
                SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun getPromptText(urgency: UrgencyLevel): String {
        val service = if (shouldUseRealService()) {
            realService
        } else {
            simulatedService
        }
        return service.getPromptText(urgency)
    }

    override suspend fun verifyVoiceResponse(
        urgency: UrgencyLevel,
        attemptCount: Int,
        timeoutMillis: Long
    ): VoiceVerificationResult {
        val service = if (shouldUseRealService()) {
            realService
        } else {
            simulatedService
        }
        return service.verifyVoiceResponse(urgency, attemptCount, timeoutMillis)
    }
}
