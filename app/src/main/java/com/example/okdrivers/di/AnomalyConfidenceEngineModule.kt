package com.example.okdrivers.di

import com.example.okdrivers.domain.engine.AnomalyConfidenceEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnomalyConfidenceEngineModule {

    @Provides
    @Singleton
    fun provideAnomalyConfidenceEngine():
            AnomalyConfidenceEngine {
        return AnomalyConfidenceEngine()
    }
}