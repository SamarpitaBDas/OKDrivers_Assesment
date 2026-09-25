package com.example.okdrivers.di

import com.example.okdrivers.domain.engine.DriverStateEngine
import com.example.okdrivers.domain.engine.SimulatorDriverStateEngine

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DriverStateEngineModule {

    @Binds
    @Singleton
    abstract fun bindDriverStateEngine(
        implementation: SimulatorDriverStateEngine
    ): DriverStateEngine
}