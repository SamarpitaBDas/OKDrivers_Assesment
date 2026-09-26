package com.example.okdrivers.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.okdrivers.data.local.dao.AnomalyEventDao
import com.example.okdrivers.data.local.dao.AIConversationSessionDao
import com.example.okdrivers.data.local.dao.DriverProfileDao
import com.example.okdrivers.data.local.dao.DriverStateDao
import com.example.okdrivers.data.local.dao.IncidentDao
import com.example.okdrivers.data.local.dao.IncidentStateTransitionDao
import com.example.okdrivers.data.local.dao.NotificationEventDao
import com.example.okdrivers.data.local.dao.ResponderActionDao
import com.example.okdrivers.data.local.dao.ResponderDao
import com.example.okdrivers.data.local.dao.SafetyBaselineDao
import com.example.okdrivers.data.local.dao.SensorSampleDao
import com.example.okdrivers.data.local.dao.SyncQueueDao
import com.example.okdrivers.data.local.dao.VehicleProfileDao
import com.example.okdrivers.data.local.dao.VehicleTelemetryDao
import com.example.okdrivers.data.local.entity.AnomalyEventEntity
import com.example.okdrivers.data.local.entity.AIConversationSessionEntity
import com.example.okdrivers.data.local.entity.DriverProfileEntity
import com.example.okdrivers.data.local.entity.DriverStateEntity
import com.example.okdrivers.data.local.entity.IncidentEntity
import com.example.okdrivers.data.local.entity.IncidentStateTransitionEntity
import com.example.okdrivers.data.local.entity.NotificationEventEntity
import com.example.okdrivers.data.local.entity.ResponderActionEntity
import com.example.okdrivers.data.local.entity.ResponderEntity
import com.example.okdrivers.data.local.entity.SafetyBaselineEntity
import com.example.okdrivers.data.local.entity.SensorSampleEntity
import com.example.okdrivers.data.local.entity.SyncQueueEntity
import com.example.okdrivers.data.local.entity.VehicleProfileEntity
import com.example.okdrivers.data.local.entity.VehicleTelemetryEntity

@Database(
    entities = [
        DriverProfileEntity::class,
        VehicleProfileEntity::class,
        SensorSampleEntity::class,
        VehicleTelemetryEntity::class,
        DriverStateEntity::class,
        SafetyBaselineEntity::class,
        AnomalyEventEntity::class,
        IncidentEntity::class,
        IncidentStateTransitionEntity::class,
        AIConversationSessionEntity::class,
        ResponderEntity::class,
        ResponderActionEntity::class,
        NotificationEventEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun driverProfileDao(): DriverProfileDao
    abstract fun vehicleProfileDao(): VehicleProfileDao
    abstract fun sensorSampleDao(): SensorSampleDao
    abstract fun vehicleTelemetryDao(): VehicleTelemetryDao
    abstract fun driverStateDao(): DriverStateDao
    abstract fun safetyBaselineDao(): SafetyBaselineDao
    abstract fun anomalyEventDao(): AnomalyEventDao
    abstract fun incidentDao(): IncidentDao
    abstract fun incidentStateTransitionDao(): IncidentStateTransitionDao
    abstract fun aiConversationSessionDao(): AIConversationSessionDao
    abstract fun responderDao(): ResponderDao
    abstract fun responderActionDao(): ResponderActionDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun syncQueueDao(): SyncQueueDao
}
