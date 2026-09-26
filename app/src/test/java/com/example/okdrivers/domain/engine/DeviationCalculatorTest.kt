package com.example.okdrivers.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviationCalculatorTest {

    @Test
    fun testNormalValueWithinBaseline() {
        val result = DeviationCalculator.computeDeviation(
            currentValue = 0.20f,
            mean = 0.20f,
            stdDev = 0.05f
        )

        assertEquals(0.0f, result.deviationScore, 0.001f)
        assertEquals(0.0f, result.confidence, 0.001f)
        assertFalse(result.isOutsideNormalRange)
    }

    @Test
    fun testElevatedDeviationZScore() {
        val result = DeviationCalculator.computeDeviation(
            currentValue = 0.35f,
            mean = 0.20f,
            stdDev = 0.05f
        )

        assertEquals(3.0f, result.deviationScore, 0.001f)
        assertEquals(1.0f, result.confidence, 0.001f)
        assertTrue(result.isOutsideNormalRange)
    }

    @Test
    fun testMinMaxRangeCheck() {
        val result = DeviationCalculator.computeDeviation(
            currentValue = 0.60f,
            mean = 0.20f,
            stdDev = 0.20f,
            minVal = 0.10f,
            maxVal = 0.50f
        )

        assertTrue(result.isOutsideNormalRange)
    }
}
