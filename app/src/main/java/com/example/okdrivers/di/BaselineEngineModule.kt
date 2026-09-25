package com.example.okdrivers.di

import com.example.okdrivers.domain.engine.BaselineEngine
import com.example.okdrivers.domain.engine.RollingBaselineEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BaselineEngineModule {

    @Binds
    @Singleton
    abstract fun bindBaselineEngine(
        implementation: RollingBaselineEngine
    ): BaselineEngine
}