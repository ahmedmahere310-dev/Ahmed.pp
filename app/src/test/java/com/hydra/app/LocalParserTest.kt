package com.hydra.app

import com.hydra.app.data.model.EventType
import com.hydra.app.domain.parser.LocalParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalParserTest {

    @Test
    fun testParseWaterEnglish() {
        val result1 = LocalParser.parse("500 ml")
        assertEquals(EventType.WATER, result1.type)
        assertEquals(500.0, result1.amount ?: 0.0, 0.01)
        assertEquals("ml", result1.unit)

        val result2 = LocalParser.parse("1000 ml")
        assertEquals(EventType.WATER, result2.type)
        assertEquals(1000.0, result2.amount ?: 0.0, 0.01)
    }

    @Test
    fun testParseWaterArabicVariants() {
        val result1 = LocalParser.parse("شربت 500 ملي")
        assertEquals(EventType.WATER, result1.type)
        assertEquals(500.0, result1.amount ?: 0.0, 0.01)

        val result2 = LocalParser.parse("شربت 700 ملية")
        assertEquals(EventType.WATER, result2.type)
        assertEquals(700.0, result2.amount ?: 0.0, 0.01)

        val result3 = LocalParser.parse("شربت لتر مية")
        assertEquals(EventType.WATER, result3.type)
        assertEquals(1000.0, result3.amount ?: 0.0, 0.01)

        val result4 = LocalParser.parse("شربت نص لتر")
        assertEquals(EventType.WATER, result4.type)
        assertEquals(500.0, result4.amount ?: 0.0, 0.01)

        val result5 = LocalParser.parse("شربت مية")
        assertEquals(EventType.WATER, result5.type)
        assertTrue((result5.amount ?: 0.0) > 0)
    }

    @Test
    fun testParseCreatine() {
        val result1 = LocalParser.parse("خدت الكرياتين دلوقتي")
        assertEquals(EventType.CREATINE, result1.type)
        assertEquals(5.0, result1.amount ?: 0.0, 0.01)

        val result2 = LocalParser.parse("أخذت كرياتين 10 جرام")
        assertEquals(EventType.CREATINE, result2.type)
        assertEquals(10.0, result2.amount ?: 0.0, 0.01)
    }

    @Test
    fun testParseFood() {
        val result1 = LocalParser.parse("أكلت الغدا رز وفراخ وسلطة")
        assertEquals(EventType.FOOD, result1.type)
        assertTrue(result1.description?.contains("رز") == true)
        assertNotNull(result1.nutrients)

        val result2 = LocalParser.parse("أكلت سندوتش")
        assertEquals(EventType.FOOD, result2.type)

        val result3 = LocalParser.parse("أكلت 2 بيضة")
        assertEquals(EventType.FOOD, result3.type)
        assertTrue(result3.nutrients?.protein ?: 0.0 > 0)
    }

    @Test
    fun testParseNoteFallback() {
        val result = LocalParser.parse("مشوار في الشغل")
        assertEquals(EventType.NOTE, result.type)
        assertEquals("مشوار في الشغل", result.description)
    }
}
