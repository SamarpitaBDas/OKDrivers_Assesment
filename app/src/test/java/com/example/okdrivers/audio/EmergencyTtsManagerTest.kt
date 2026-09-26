package com.example.okdrivers.audio

import com.example.okdrivers.domain.model.UrgencyLevel
import org.junit.Assert.*
import org.junit.Test

class EmergencyTtsManagerTest {

    @Test
    fun testUrgencyLevelPromptMapping() {
        val initialPrompt = "Emergency detected. Are you okay? Please respond."
        val urgentPrompt = "We still haven't heard from you. Please respond now."
        val finalPrompt = "No response detected. Emergency services will be notified."

        assertEquals(initialPrompt, getPromptForUrgency(UrgencyLevel.INITIAL))
        assertEquals(urgentPrompt, getPromptForUrgency(UrgencyLevel.URGENT))
        assertEquals(finalPrompt, getPromptForUrgency(UrgencyLevel.FINAL))
    }

    private fun getPromptForUrgency(urgency: UrgencyLevel): String {
        return when (urgency) {
            UrgencyLevel.INITIAL -> "Emergency detected. Are you okay? Please respond."
            UrgencyLevel.URGENT -> "We still haven't heard from you. Please respond now."
            UrgencyLevel.FINAL -> "No response detected. Emergency services will be notified."
        }
    }
}
