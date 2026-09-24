package com.hydra.app.domain.parser

import com.hydra.app.data.model.EventSource
import com.hydra.app.data.model.EventType
import com.hydra.app.data.model.FoodNutrients
import com.hydra.app.data.model.ParsedInput
import java.util.regex.Pattern

object LocalParser {

    fun parse(input: String, source: EventSource = EventSource.MANUAL): ParsedInput {
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

        // 1. Water Parsing Check
        val waterResult = tryParseWater(trimmed, source)
        if (waterResult != null) return waterResult

        // 2. Bathroom Parsing Check
        val bathroomResult = tryParseBathroom(trimmed, source)
        if (bathroomResult != null) return bathroomResult

        // 3. Creatine Parsing Check
        val creatineResult = tryParseCreatine(trimmed, source)
        if (creatineResult != null) return creatineResult

        // 4. Food Parsing Check
        val foodResult = tryParseFood(trimmed, source)
        if (foodResult != null) return foodResult

        // 5. Fallback to Note
        return ParsedInput(
            type = EventType.NOTE,
            description = trimmed,
            confidence = 0.5,
            source = source,
            rawText = input
        )
    }

    private fun tryParseBathroom(text: String, source: EventSource): ParsedInput? {
        val lower = text.lowercase()
        val bathroomKeywords = listOf(
            "دخلت الحمام", "الحمام", "حمام", "دخول الحمام", "toilet", "bathroom",
            "تبول", "بول", "براز", "wc", "قضيت حاجتي", "رحت الحمام", "خرجت من الحمام"
        )

        val hasKeyword = bathroomKeywords.any { lower.contains(it) }
        if (!hasKeyword) return null

        return ParsedInput(
            type = EventType.BATHROOM,
            amount = null,
            unit = null,
            description = "دخول الحمام",
            confidence = 0.98,
            source = source,
            rawText = text
        )
    }

    private fun tryParseWater(text: String, source: EventSource): ParsedInput? {
        val lower = text.lowercase()

        // Keywords for water
        val hasWaterKeyword = lower.contains("شربت") ||
                lower.contains("شرب") ||
                lower.contains("مية") ||
                lower.contains("ماي") ||
                lower.contains("ماء") ||
                lower.contains("water") ||
                lower.contains("drink") ||
                lower.contains("ازازة") ||
                lower.contains("زجاجة") ||
                lower.contains("كوب") ||
                lower.contains("كاسة")

        val hasMlUnit = lower.contains("مل") ||
                lower.contains("ملي") ||
                lower.contains("ملية") ||
                lower.contains("مليلتر") ||
                lower.contains("ml") ||
                lower.contains("لتر") ||
                lower.contains("liter") ||
                lower.contains("litre")

        if (!hasWaterKeyword && !hasMlUnit) {
            return null
        }

        // Check for specific liter expressions
        var amount: Double? = null

        when {
            lower.contains("نص لتر") || lower.contains("نصف لتر") -> amount = 500.0
            lower.contains("ربع لتر") -> amount = 250.0
            lower.contains("2 لتر") || lower.contains("اتنين لتر") || lower.contains("لترين") -> amount = 2000.0
            lower.contains("لتر ونصف") || lower.contains("لتر ونص") -> amount = 1500.0
            lower.contains("لتر") || lower.contains("liter") || lower.contains("1 لتر") -> amount = 1000.0
        }

        if (amount == null) {
            // Extract numbers using regex
            val numberRegex = Pattern.compile("(\\d+(\\.\\d+)?)")
            val matcher = numberRegex.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)
                val parsedNum = numStr?.toDoubleOrNull()
                if (parsedNum != null) {
                    if (lower.contains("لتر") || lower.contains("liter") || lower.contains("l")) {
                        amount = if (parsedNum <= 10) parsedNum * 1000 else parsedNum
                    } else {
                        amount = parsedNum
                    }
                }
            }
        }

        // If water was mentioned without an explicit number
        if (amount == null) {
            amount = when {
                lower.contains("ازازة") || lower.contains("زجاجة") || lower.contains("قارورة") -> 500.0
                lower.contains("كوب") || lower.contains("كاسة") || lower.contains("كوباية") -> 250.0
                hasWaterKeyword -> 250.0 // Default glass
                else -> null
            }
        }

        if (amount != null && amount > 0) {
            return ParsedInput(
                type = EventType.WATER,
                amount = amount,
                unit = "ml",
                description = "ماء",
                confidence = if (hasMlUnit || hasWaterKeyword) 0.95 else 0.8,
                source = source,
                rawText = text
            )
        }

        return null
    }

    private fun tryParseCreatine(text: String, source: EventSource): ParsedInput? {
        val lower = text.lowercase()
        val hasCreatine = lower.contains("كرياتين") ||
                lower.contains("الكرياتين") ||
                lower.contains("creatine")

        if (!hasCreatine) return null

        // Check if amount is specified (e.g. 5 جرام)
        val numberRegex = Pattern.compile("(\\d+(\\.\\d+)?)")
        val matcher = numberRegex.matcher(text)
        val amount = if (matcher.find()) matcher.group(1)?.toDoubleOrNull() else 5.0

        return ParsedInput(
            type = EventType.CREATINE,
            amount = amount ?: 5.0,
            unit = "g",
            description = "كرياتين",
            confidence = 0.98,
            source = source,
            rawText = text
        )
    }

    private fun tryParseFood(text: String, source: EventSource): ParsedInput? {
        val lower = text.lowercase()
        val foodVerbs = listOf("أكلت", "اكلت", "أكل", "اكل", "تناولت", "فطرت", "اتغديت", "تعشيت", "وجبة", "سناك", "ate", "eat", "food", "breakfast", "lunch", "dinner")
        val foodItems = listOf("رز", "أرز", "فراخ", "دجاج", "لحمة", "لحم", "سلطة", "سندوتش", "ساندوتش", "بيض", "بيضة", "بيضتين", "تونة", "سمك", "شوفان", "لبن", "زبادي", "تفاح", "موز", "خبز", "عيش", "بروتين")

        val hasVerb = foodVerbs.any { lower.contains(it) }
        val hasFoodItem = foodItems.any { lower.contains(it) }

        if (!hasVerb && !hasFoodItem) {
            return null
        }

        // Clean description
        var description = text
        for (verb in foodVerbs) {
            description = description.replace(verb, "", ignoreCase = true)
        }
        description = description.replace("الغدا", "")
            .replace("العشا", "")
            .replace("الفطار", "")
            .trim()

        if (description.isEmpty()) {
            description = text.trim()
        }

        // Estimate approximate nutrition
        val nutrients = estimateApproximateNutrients(description)

        return ParsedInput(
            type = EventType.FOOD,
            amount = null,
            unit = null,
            description = description,
            nutrients = nutrients,
            confidence = 0.92,
            source = source,
            rawText = text
        )
    }

    private fun estimateApproximateNutrients(foodText: String): FoodNutrients? {
        val lower = foodText.lowercase()
        var cal = 0
        var pro = 0.0
        var carb = 0.0
        var fat = 0.0
        var matched = false

        if (lower.contains("بيضتين") || lower.contains("2 بيض") || lower.contains("2 بيضة")) {
            cal += 150; pro += 12.0; carb += 1.0; fat += 10.0; matched = true
        } else if (lower.contains("بيضة") || lower.contains("بيض")) {
            cal += 75; pro += 6.0; carb += 0.5; fat += 5.0; matched = true
        }

        if (lower.contains("رز") || lower.contains("أرز")) {
            cal += 200; pro += 4.0; carb += 45.0; fat += 1.0; matched = true
        }

        if (lower.contains("فراخ") || lower.contains("دجاج") || lower.contains("صدر دجاج")) {
            cal += 220; pro += 35.0; carb += 0.0; fat += 4.0; matched = true
        }

        if (lower.contains("سلطة") || lower.contains("خضار")) {
            cal += 50; pro += 2.0; carb += 8.0; fat += 1.0; matched = true
        }

        if (lower.contains("سندوتش") || lower.contains("ساندوتش")) {
            cal += 320; pro += 15.0; carb += 35.0; fat += 12.0; matched = true
        }

        if (lower.contains("شوفان")) {
            cal += 180; pro += 6.0; carb += 30.0; fat += 3.0; matched = true
        }

        if (lower.contains("تونة")) {
            cal += 150; pro += 30.0; carb += 0.0; fat += 2.0; matched = true
        }

        return if (matched) {
            FoodNutrients(calories = cal, protein = pro, carbs = carb, fat = fat)
        } else {
            null
        }
    }
}
