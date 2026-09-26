package com.example.okdrivers.domain.community

import com.example.okdrivers.domain.model.Responder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ResponderSearchService @Inject constructor() {

    fun findEligible(
        responders: List<Responder>,
        incidentLat: Double,
        incidentLng: Double,
        radiusKm: Double
    ): List<Responder> {
        val mapped = responders.filter { it.isActive }.map { responder ->
            val dist = ResponderDistanceCalculator.distanceKm(
                Pair(incidentLat, incidentLng),
                Pair(responder.latitude, responder.longitude)
            )
            Triple(responder, dist, responder.reputationScore ?: 3.5f)
        }.filter { it.second <= radiusKm }

        return mapped.sortedWith(compareBy<Triple<Responder, Double, Float>> { it.second }
            .thenComparing { a, b ->
                if (abs(a.second - b.second) <= 0.5f) {
                    b.third.compareTo(a.third)
                } else {
                    0
                }
            }).map { it.first }
    }
}
