package com.example.okdrivers.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SamplingRateTest {

    @Test
    fun testSamplingRatePersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SamplingRateRepository(context)

        // Default should be NORMAL
        val initial = repository.samplingRateFlow.first()
        assertEquals(SamplingRate.NORMAL, initial)

        // Set to BATTERY_SAVER
        repository.setSamplingRate(SamplingRate.BATTERY_SAVER)
        val updated = repository.samplingRateFlow.first()
        assertEquals(SamplingRate.BATTERY_SAVER, updated)

        // Reset to NORMAL for cleanliness
        repository.setSamplingRate(SamplingRate.NORMAL)
    }
}
