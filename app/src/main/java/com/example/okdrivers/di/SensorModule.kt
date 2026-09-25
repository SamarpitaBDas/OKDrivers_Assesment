package com.example.okdrivers.di

import com.example.okdrivers.sensors.MotionSensorManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.Provides

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {
    @Provides
    fun provideMotionSensorManager(
        manager: MotionSensorManager
    ): MotionSensorManager {
        return manager
    }
}