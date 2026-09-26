package com.example.okdrivers.domain.community

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ResponderDistanceCalculator {
    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(from: Pair<Double, Double>, to: Pair<Double, Double>): Double {
        val lat1 = Math.toRadians(from.first)
        val lon1 = Math.toRadians(from.second)
        val lat2 = Math.toRadians(to.first)
        val lon2 = Math.toRadians(to.second)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))

        return EARTH_RADIUS_KM * c
    }
}
