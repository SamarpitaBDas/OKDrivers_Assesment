package com.example.okdrivers.sensors

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkStatusManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val connectivityManager =
        context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    fun observeNetwork(): Flow<NetworkStatusSample> =
        callbackFlow {

            fun emitCurrentStatus() {

                val network =
                    connectivityManager.activeNetwork

                val capabilities =
                    network?.let {
                        connectivityManager.getNetworkCapabilities(it)
                    }

                val isOnline =
                    capabilities?.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ) == true &&
                            capabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_VALIDATED
                            )

                trySend(
                    NetworkStatusSample(
                        timestamp = System.currentTimeMillis(),
                        isOnline = isOnline
                    )
                )
            }

            val networkCallback =
                object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(
                        network: Network
                    ) {
                        emitCurrentStatus()
                    }

                    override fun onLost(
                        network: Network
                    ) {
                        emitCurrentStatus()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        emitCurrentStatus()
                    }
                }

            val request =
                NetworkRequest.Builder()
                    .addCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    .build()

            connectivityManager.registerNetworkCallback(
                request,
                networkCallback
            )

            // Emit the current state immediately.
            emitCurrentStatus()

            awaitClose {
                connectivityManager.unregisterNetworkCallback(
                    networkCallback
                )
            }
        }
}