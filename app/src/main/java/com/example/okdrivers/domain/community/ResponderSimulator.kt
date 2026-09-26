package com.example.okdrivers.domain.community

import com.example.okdrivers.domain.model.Responder
import com.example.okdrivers.domain.model.ResponderStatus
import kotlin.random.Random

object ResponderSimulator {

    private val fixtureResponders = listOf(
        Pair("Dr. Sarah Miller", 4.8f),
        Pair("Medic John Doe", 4.6f),
        Pair("Officer Robert Chen", 4.3f),
        Pair("Paramedic Emily Watson", 4.9f)
    )

    private val randomNames = listOf(
        "Alex Rivera", "Jordan Smith", "Taylor Swift", "Morgan Freeman",
        "Casey Jones", "Pat Davis", "Sam Wilson", "Avery Brown"
    )

    fun generateAround(centerLat: Double, centerLng: Double, count: Int = 6): List<Responder> {
        val responders = mutableListOf<Responder>()
        val random = Random(42) // Deterministic seed for demo reproducibility

        // 1. Add fixture responders
        for ((index, fixture) in fixtureResponders.take(minOf(count, fixtureResponders.size)).withIndex()) {
            val latOffset = (random.nextDouble() - 0.5) * 0.05 // ~5km radius
            val lngOffset = (random.nextDouble() - 0.5) * 0.05
            responders.add(
                Responder(
                    id = "responder_fixture_$index",
                    name = fixture.first,
                    latitude = centerLat + latOffset,
                    longitude = centerLng + lngOffset,
                    isActive = true,
                    reputationScore = fixture.second,
                    status = ResponderStatus.AVAILABLE
                )
            )
        }

        // 2. Add random filler responders if count > fixtures
        val remaining = count - responders.size
        for (i in 0 until remaining) {
            val name = randomNames.random(random)
            val latOffset = (random.nextDouble() - 0.5) * 0.08
            val lngOffset = (random.nextDouble() - 0.5) * 0.08
            val reputation = 3.5f + random.nextFloat() * 1.5f // 3.5 - 5.0
            responders.add(
                Responder(
                    id = "responder_rand_$i",
                    name = "$name (${i + 1})",
                    latitude = centerLat + latOffset,
                    longitude = centerLng + lngOffset,
                    isActive = true,
                    reputationScore = String.format(java.util.Locale.US, "%.1f", reputation).toFloat(),
                    status = ResponderStatus.AVAILABLE
                )
            )
        }

        return responders
    }
}
