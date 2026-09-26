package com.example.okdrivers.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.okdrivers.MainActivity
import com.example.okdrivers.R
import com.example.okdrivers.data.repository.SamplingRateRepository
import com.example.okdrivers.domain.engine.AnomalyManager
import com.example.okdrivers.sensors.GpsLocationManager
import com.example.okdrivers.sensors.MotionSensorManager
import com.example.okdrivers.sensors.VehicleTelemetrySimulator
import com.example.okdrivers.domain.engine.EmergencyOrchestratorCoordinator
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.EmergencyState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SafetyMonitoringService : Service() {

    @Inject
    lateinit var samplingRateRepository: SamplingRateRepository

    @Inject
    lateinit var motionSensorManager: MotionSensorManager

    @Inject
    lateinit var gpsLocationManager: GpsLocationManager

    @Inject
    lateinit var telemetrySimulator: VehicleTelemetrySimulator

    @Inject
    lateinit var anomalyManager: AnomalyManager

    @Inject
    lateinit var coordinator: EmergencyOrchestratorCoordinator

    @Inject
    lateinit var incidentStateMachine: IncidentStateMachine

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "safety_monitoring_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, SafetyMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SafetyMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification("Monitoring active")
        startForeground(NOTIFICATION_ID, notification)

        // Observe state changes and launch orchestrator jobs via coordinator
        serviceScope.launch {
            incidentStateMachine.currentState.collect { state ->
                val activeIncident = incidentStateMachine.getActiveIncident()
                if (activeIncident != null) {
                    when (state) {
                        EmergencyState.AI_VERIFICATION -> {
                            coordinator.launchVerification(
                                incidentId = activeIncident.id,
                                driverId = activeIncident.driverId,
                                vehicleId = activeIncident.vehicleId,
                                severity = activeIncident.severity
                            )
                        }
                        EmergencyState.COMMUNITY_MOBILIZATION -> {
                            val lat = activeIncident.latitude ?: 37.7749
                            val lng = activeIncident.longitude ?: -122.4194
                            coordinator.launchMobilization(
                                incidentId = activeIncident.id,
                                lat = lat,
                                lng = lng
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        // Observe sampling rate and collect sensor feeds
        serviceScope.launch {
            samplingRateRepository.samplingRateFlow.collectLatest { rate ->
                // Monitor sensor streams using rate.intervalMillis
                launch {
                    motionSensorManager.observeMotion().collect { sample ->
                        // Delegate to domain manager
                        anomalyManager.evaluateAndPersist(
                            driverId = "driver_1",
                            vehicleId = "vehicle_1",
                            vehicleTelemetry = null,
                            driverState = null,
                            motionSensor = sample,
                            gpsLocation = null,
                            driverBaseline = null,
                            vehicleBaseline = null
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safety Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps okDriver safety telemetry monitoring active in background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("okDriver Active")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
