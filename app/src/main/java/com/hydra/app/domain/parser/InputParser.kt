package com.hydra.app.domain.parser

import com.hydra.app.data.model.AiProvider
import com.hydra.app.data.model.EventSource
import com.hydra.app.data.model.EventType
import com.hydra.app.data.model.ParsedInput
import com.hydra.app.domain.ai.AIService

class InputParser(
    private val aiService: AIService
) {
    suspend fun parse(
        input: String,
        source: EventSource = EventSource.MANUAL,
        aiProvider: AiProvider = AiProvider.OFF,
        apiKey: String = ""
    ): ParsedInput {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedInput(
                type = EventType.NOTE,
                description = "",
                confidence = 0.0,
                source = source,
                rawText = input
            )
        }

        // 1. Try LocalParser first
        val localResult = LocalParser.parse(trimmed, source)

        // If local parser is highly confident (e.g. recognized Water, Creatine, or known Food)
        if (localResult.confidence >= 0.85) {
            return localResult
        }

        // 2. If AI is enabled and we have a key, try AI
        if (aiProvider == AiProvider.GEMINI && apiKey.isNotBlank()) {
            val aiResult = aiService.parseNaturalLanguage(trimmed, apiKey)
            if (aiResult.isSuccess) {
                return aiResult.getOrThrow()
            }
        }

        // 3. Graceful fallback to LocalParser result
        return localResult
    }
}
