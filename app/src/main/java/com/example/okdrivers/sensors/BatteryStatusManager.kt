package com.example.okdrivers.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BatteryStatusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun observeBattery(): Flow<BatteryStatusSample> = callbackFlow {

        val batteryReceiver = object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) {
                    return
                }

                val level = intent.getIntExtra(
                    BatteryManager.EXTRA_LEVEL,
                    -1
                )

                val scale = intent.getIntExtra(
                    BatteryManager.EXTRA_SCALE,
                    -1
                )

                val status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    -1
                )

                val percentage =
                    if (level >= 0 && scale > 0) {
                        (level * 100) / scale
                    } else {
                        0
                    }

                val isCharging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                trySend(
                    BatteryStatusSample(
                        timestamp = System.currentTimeMillis(),
                        batteryPercentage = percentage,
                        isCharging = isCharging
                    )
                )
            }
        }

        val filter = IntentFilter(
            Intent.ACTION_BATTERY_CHANGED
        )

        context.registerReceiver(
            batteryReceiver,
            filter
        )

        awaitClose {
            context.unregisterReceiver(
                batteryReceiver
            )
        }
    }
}