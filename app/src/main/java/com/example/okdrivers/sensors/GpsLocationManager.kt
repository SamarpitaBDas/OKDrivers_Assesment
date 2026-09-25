package com.example.okdrivers.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GpsLocationManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)
    @SuppressLint("MissingPermission")
    fun observeLocation(
        intervalMillis: Long = 1000L
    ): Flow<GpsLocationSample> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        )
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(
                result: LocationResult
            ) {
                for (location in result.locations) {
                    val sample = GpsLocationSample(
                        timestamp = location.time,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speedMetersPerSecond =
                            if (location.hasSpeed()) {
                                location.speed
                            } else {
                                0f
                            },
                        headingDegrees =
                            if (location.hasBearing()) {
                                location.bearing
                            } else {
                                0f
                            }
                    )
                    trySend(sample)
                }
            }
        }
        fusedLocationClient
            .requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            .addOnFailureListener { exception ->
                close(exception)
            }
        awaitClose {
            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }
    }
}