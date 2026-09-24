package com.hydra.app.domain.ai

import com.hydra.app.data.model.EventSource
import com.hydra.app.data.model.EventType
import com.hydra.app.data.model.FoodNutrients
import com.hydra.app.data.model.ParsedInput
import java.util.regex.Pattern

object AIJsonParser {

    fun parse(jsonString: String, rawInput: String): Result<ParsedInput> {
        return try {
            // Strip any markdown code fence if present
            var clean = jsonString.trim()
            if (clean.startsWith("```json")) {
                clean = clean.removePrefix("```json")
            } else if (clean.startsWith("```")) {
                clean = clean.removePrefix("```")
            }
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```")
            }
            clean = clean.trim()

            if (!clean.startsWith("{") || !clean.endsWith("}")) {
                return Result.failure(IllegalArgumentException("Invalid JSON format"))
            }

            val typeStr = extractString(clean, "type")?.uppercase() ?: "NOTE"
            val eventType = try {
                EventType.valueOf(typeStr)
            } catch (e: Exception) {
                EventType.NOTE
            }

            val amount = extractDouble(clean, "amount")
            val unit = extractString(clean, "unit")
            val description = extractString(clean, "description")
            val calories = extractInt(clean, "calories")
            val protein = extractDouble(clean, "protein")
            val carbs = extractDouble(clean, "carbs")
            val fat = extractDouble(clean, "fat")
            val confidence = extractDouble(clean, "confidence") ?: 0.9

            val nutrients = if (calories != null || protein != null || carbs != null || fat != null) {
                FoodNutrients(calories = calories, protein = protein, carbs = carbs, fat = fat)
            } else null

            val parsed = ParsedInput(
                type = eventType,
                amount = amount,
                unit = unit,
                description = description ?: rawInput,
                nutrients = nutrients,
                confidence = confidence.coerceIn(0.0, 1.0),
                source = EventSource.AI,
                rawText = rawInput
            )
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*([0-9]+(\\.[0-9]+)?)")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1)?.toDoubleOrNull() else null
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*([0-9]+)")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1)?.toIntOrNull() else null
    }
}
