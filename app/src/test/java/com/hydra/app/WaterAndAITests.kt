package com.hydra.app

import com.hydra.app.data.local.EventEntity
import com.hydra.app.data.model.EventType
import com.hydra.app.domain.ai.AIJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterCalculationTest {

    @Test
    fun testDailyWaterSum() {
        val events = listOf(
            EventEntity(type = EventType.WATER.name, amount = 500.0),
            EventEntity(type = EventType.WATER.name, amount = 250.0),
            EventEntity(type = EventType.FOOD.name, amount = null),
            EventEntity(type = EventType.WATER.name, amount = 750.0)
        )

        val totalWater = events.filter { it.type == EventType.WATER.name }
            .sumOf { it.amount?.toInt() ?: 0 }

        assertEquals(1500, totalWater)
    }

    @Test
    fun testGoalProgressCalculation() {
        val totalWater = 2000
        val goal = 2500
        val progress = (totalWater.toFloat() / goal).coerceIn(0f, 1f)

        assertEquals(0.8f, progress, 0.001f)
    }
}

class AIJsonParserTest {

    @Test
    fun testValidJsonParsing() {
        val json = """
            {
              "type": "WATER",
              "amount": 750,
              "unit": "ml",
              "description": "ماء",
              "confidence": 0.95
            }
        """.trimIndent()

        val result = AIJsonParser.parse(json, "شربت 750 مل")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(EventType.WATER, parsed.type)
        assertEquals(750.0, parsed.amount ?: 0.0, 0.01)
    }

    @Test
    fun testMarkdownWrappedJsonParsing() {
        val jsonWithFences = """
            ```json
            {
              "type": "FOOD",
              "description": "رز وفراخ",
              "calories": 420,
              "protein": 35.0,
              "confidence": 0.9
            }
            ```
        """.trimIndent()

        val result = AIJsonParser.parse(jsonWithFences, "أكلت رز وفراخ")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(EventType.FOOD, parsed.type)
        assertEquals(420, parsed.nutrients?.calories)
        assertEquals(35.0, parsed.nutrients?.protein ?: 0.0, 0.01)
    }

    @Test
    fun testMalformedJsonHandling() {
        val brokenJson = "not a json string"
        val result = AIJsonParser.parse(brokenJson, "أي كلام")
        assertTrue(result.isFailure)
    }
}
