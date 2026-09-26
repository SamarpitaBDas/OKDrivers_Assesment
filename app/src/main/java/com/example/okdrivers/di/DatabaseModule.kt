package com.example.okdrivers.di

import android.content.Context
import androidx.room.Room
import com.example.okdrivers.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "okdriver_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    @Provides
    fun provideDriverProfileDao(database: AppDatabase) =
        database.driverProfileDao()
    @Provides
    fun provideVehicleProfileDao(database: AppDatabase) =
        database.vehicleProfileDao()
    @Provides
    fun provideSensorSampleDao(database: AppDatabase) =
        database.sensorSampleDao()
    @Provides
    fun provideVehicleTelemetryDao(database: AppDatabase) =
        database.vehicleTelemetryDao()
    @Provides
    fun provideDriverStateDao(database: AppDatabase) =
        database.driverStateDao()
    @Provides
    fun provideSafetyBaselineDao(database: AppDatabase) =
        database.safetyBaselineDao()
    @Provides
    fun provideAnomalyEventDao(database: AppDatabase) =
        database.anomalyEventDao()
    @Provides
    fun provideIncidentDao(database: AppDatabase) =
        database.incidentDao()
    @Provides
    fun provideIncidentStateTransitionDao(database: AppDatabase) =
        database.incidentStateTransitionDao()
    @Provides
    fun provideAIConversationSessionDao(database: AppDatabase) =
        database.aiConversationSessionDao()
    @Provides
    fun provideResponderDao(database: AppDatabase) =
        database.responderDao()
    @Provides
    fun provideResponderActionDao(database: AppDatabase) =
        database.responderActionDao()
    @Provides
    fun provideNotificationEventDao(database: AppDatabase) =
        database.notificationEventDao()
    @Provides
    fun provideSyncQueueDao(database: AppDatabase) =
        database.syncQueueDao()
}
