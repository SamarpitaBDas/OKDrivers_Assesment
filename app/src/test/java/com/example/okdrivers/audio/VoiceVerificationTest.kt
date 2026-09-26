package com.example.okdrivers.audio

import com.example.okdrivers.domain.model.ResponseClassification
import org.junit.Assert.*
import org.junit.Test

class VoiceVerificationTest {

    @Test
    fun testClassificationLogic() {
        assertEquals(ResponseClassification.RESPONSIVE, classify("I'm okay"))
        assertEquals(ResponseClassification.RESPONSIVE, classify("yes help me"))
        assertEquals(ResponseClassification.IMPAIRED, classify("asdfghjkl"))
        assertEquals(ResponseClassification.UNRESPONSIVE, classify(null))
        assertEquals(ResponseClassification.UNRESPONSIVE, classify(""))
    }

    private fun classify(transcript: String?): ResponseClassification {
        if (transcript.isNullOrBlank()) {
            return ResponseClassification.UNRESPONSIVE
        }
        val affirmativeKeywords = listOf("ok", "okay", "yes", "help", "fine", "yeah", "sure", "i'm okay", "i am okay")
        val cleaned = transcript.trim().lowercase()
        val isAffirmative = affirmativeKeywords.any { cleaned.contains(it) }
        return if (isAffirmative) {
            ResponseClassification.RESPONSIVE
        } else {
            ResponseClassification.IMPAIRED
        }
    }
}
