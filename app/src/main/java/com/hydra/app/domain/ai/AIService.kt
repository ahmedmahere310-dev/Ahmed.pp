package com.hydra.app.domain.ai

import com.hydra.app.data.model.ParsedInput

interface AIService {
    suspend fun parseNaturalLanguage(input: String, apiKey: String): Result<ParsedInput>
    suspend fun testConnection(apiKey: String): Result<Boolean>
}
