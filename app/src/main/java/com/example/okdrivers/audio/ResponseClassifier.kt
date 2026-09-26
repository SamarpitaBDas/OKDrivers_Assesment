package com.example.okdrivers.audio

import com.example.okdrivers.domain.model.ResponseClassification

object ResponseClassifier {
    fun classify(transcript: String?): ResponseClassification {
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
