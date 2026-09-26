package com.example.okdrivers.di

import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.AIConversationRepositoryImpl
import com.example.okdrivers.data.repository.AnomalyRepository
import com.example.okdrivers.data.repository.AnomalyRepositoryImpl
import com.example.okdrivers.data.repository.BaselineRepository
import com.example.okdrivers.data.repository.BaselineRepositoryImpl
import com.example.okdrivers.data.repository.CurrentProfileRepository
import com.example.okdrivers.data.repository.CurrentProfileRepositoryImpl
import com.example.okdrivers.data.repository.DriverRepository
import com.example.okdrivers.data.repository.DriverRepositoryImpl
import com.example.okdrivers.data.repository.DriverStateRepository
import com.example.okdrivers.data.repository.DriverStateRepositoryImpl
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentRepositoryImpl
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepositoryImpl
import com.example.okdrivers.data.repository.NotificationRepository
import com.example.okdrivers.data.repository.NotificationRepositoryImpl
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.data.repository.ResponderActionRepositoryImpl
import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.data.repository.ResponderRepositoryImpl
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.data.repository.SensorRepositoryImpl
import com.example.okdrivers.data.repository.TelemetryRepository
import com.example.okdrivers.data.repository.TelemetryRepositoryImpl
import com.example.okdrivers.data.repository.VehicleRepository
import com.example.okdrivers.data.repository.VehicleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDriverRepository(
        implementation: DriverRepositoryImpl
    ): DriverRepository

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        implementation: VehicleRepositoryImpl
    ): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        implementation: SensorRepositoryImpl
    ): SensorRepository

    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(
        implementation: TelemetryRepositoryImpl
    ): TelemetryRepository

    @Binds
    @Singleton
    abstract fun bindDriverStateRepository(
        implementation: DriverStateRepositoryImpl
    ): DriverStateRepository

    @Binds
    @Singleton
    abstract fun bindBaselineRepository(
        implementation: BaselineRepositoryImpl
    ): BaselineRepository

    @Binds
    @Singleton
    abstract fun bindAnomalyRepository(
        implementation: AnomalyRepositoryImpl
    ): AnomalyRepository

    @Binds
    @Singleton
    abstract fun bindIncidentRepository(
        implementation: IncidentRepositoryImpl
    ): IncidentRepository

    @Binds
    @Singleton
    abstract fun bindIncidentTimelineRepository(
        implementation: IncidentTimelineRepositoryImpl
    ): IncidentTimelineRepository

    @Binds
    @Singleton
    abstract fun bindAIConversationRepository(
        implementation: AIConversationRepositoryImpl
    ): AIConversationRepository

    @Binds
    @Singleton
    abstract fun bindResponderRepository(
        implementation: ResponderRepositoryImpl
    ): ResponderRepository

    @Binds
    @Singleton
    abstract fun bindResponderActionRepository(
        implementation: ResponderActionRepositoryImpl
    ): ResponderActionRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        implementation: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindCurrentProfileRepository(
        implementation: CurrentProfileRepositoryImpl
    ): CurrentProfileRepository
}