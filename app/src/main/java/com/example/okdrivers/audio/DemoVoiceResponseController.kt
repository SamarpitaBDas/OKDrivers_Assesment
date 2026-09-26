package com.example.okdrivers.audio

interface DemoVoiceResponseController {
    fun setNextSimulatedResponse(transcript: String?)
    fun setForceSimulatedMode(enabled: Boolean)
    fun isForceSimulatedMode(): Boolean
}
