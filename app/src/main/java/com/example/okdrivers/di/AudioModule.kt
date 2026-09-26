package com.example.okdrivers.di

import com.example.okdrivers.audio.DemoVoiceResponseController
import com.example.okdrivers.audio.VoiceVerificationRouter
import com.example.okdrivers.audio.VoiceVerificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindVoiceVerificationService(
        router: VoiceVerificationRouter
    ): VoiceVerificationService

    @Binds
    @Singleton
    abstract fun bindDemoVoiceResponseController(
        router: VoiceVerificationRouter
    ): DemoVoiceResponseController
}
