package com.hydra.app

import com.hydra.app.data.local.PredictionSampleEntity
import com.hydra.app.data.model.BathroomUrgeType
import com.hydra.app.data.model.EventType
import com.hydra.app.domain.parser.LocalParser
import com.hydra.app.domain.prediction.PredictionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BathroomPredictionEngineTest {

    @Test
    fun testWaterDiuresisCalculation() {
        // Smaller water volume (e.g. 200ml) should give a longer delay than large volume (e.g. 1000ml)
        val delay200 = PredictionEngine.calculateDiuresisDelay(200)
        val delay500 = PredictionEngine.calculateDiuresisDelay(500)
        val delay1000 = PredictionEngine.calculateDiuresisDelay(1000)

        assertTrue("Smaller water amount should have longer latency ($delay200 vs $delay500)", delay200 >= delay500)
        assertTrue("500ml should have longer latency than 1000ml ($delay500 vs $delay1000)", delay500 >= delay1000)
        assertTrue("Diuresis delay should be within reasonable bounds (25-95 min)", delay500 in 25..95)
    }

    @Test
    fun testGastrocolicCaloriesCalculation() {
        // Light snack (< 200 kcal) should give slower gastrocolic reflex than heavy meal (800+ kcal)
        val snackDelay = PredictionEngine.calculateGastrocolicDelay(150)
        val moderateMealDelay = PredictionEngine.calculateGastrocolicDelay(450)
        val heavyMealDelay = PredictionEngine.calculateGastrocolicDelay(900)

        assertTrue("Snack should have longer latency than moderate meal ($snackDelay vs $moderateMealDelay)", snackDelay > moderateMealDelay)
        assertTrue("Moderate meal should have longer latency than heavy meal ($moderateMealDelay vs $heavyMealDelay)", moderateMealDelay > heavyMealDelay)
        assertTrue("Heavy meal gastrocolic delay should be fast (25-45 min)", heavyMealDelay in 25..45)
    }

    @Test
    fun testCombinedPredictionResult() {
        // Both water and calories are significant -> UrgeType should be COMBINED
        val resultCombined = PredictionEngine.calculateBathroomPrediction(
            waterAmountMl = 500,
            calories = 650,
            recentSamples = emptyList()
        )

        assertEquals(BathroomUrgeType.COMBINED, resultCombined.urgeType)
        assertEquals(500, resultCombined.waterAmountMl)
        assertEquals(650, resultCombined.caloriesAmount)
        assertTrue("Explanation should be in Arabic and mention water and calories",
            resultCombined.explanationArabic.contains("500") && resultCombined.explanationArabic.contains("650"))
    }

    @Test
    fun testWaterOnlyPrediction() {
        val result = PredictionEngine.calculateBathroomPrediction(
            waterAmountMl = 400,
            calories = 0,
            recentSamples = emptyList()
        )

        assertEquals(BathroomUrgeType.URINATION, result.urgeType)
        assertTrue(result.explanationArabic.contains("إدرار البول") || result.explanationArabic.contains("المثانة"))
    }

    @Test
    fun testFoodOnlyPrediction() {
        val result = PredictionEngine.calculateBathroomPrediction(
            waterAmountMl = 0,
            calories = 600,
            recentSamples = emptyList()
        )

        assertEquals(BathroomUrgeType.DIGESTION, result.urgeType)
        assertTrue(result.explanationArabic.contains("القولوني") || result.explanationArabic.contains("الهضم"))
    }

    @Test
    fun testPreFillDiscount() {
        // If user hasn't visited bathroom in 90 minutes, delay should be shorter than when they just visited
        val delayFresh = PredictionEngine.calculateDiuresisDelay(500, minutesSinceLastBathroom = 5)
        val delayPreFilled = PredictionEngine.calculateDiuresisDelay(500, minutesSinceLastBathroom = 90)

        assertTrue("Pre-filled bladder should trigger earlier ($delayPreFilled < $delayFresh)", delayPreFilled < delayFresh)
    }

    @Test
    fun testPersonalAdaptiveLearning() {
        // Simulate a user who consistently goes after 40 minutes for 500ml water
        val sample1 = PredictionSampleEntity(
            waterAmount = 500,
            caloriesAmount = 0,
            predictedDelayMinutes = 55,
            actualDelayMinutes = 38,
            feedbackGiven = true
        )
        val sample2 = PredictionSampleEntity(
            waterAmount = 500,
            caloriesAmount = 0,
            predictedDelayMinutes = 50,
            actualDelayMinutes = 40,
            feedbackGiven = true
        )

        val learnedResult = PredictionEngine.calculateBathroomPrediction(
            waterAmountMl = 500,
            calories = 0,
            recentSamples = listOf(sample1, sample2),
            learningEnabled = true
        )

        val unlearnedResult = PredictionEngine.calculateBathroomPrediction(
            waterAmountMl = 500,
            calories = 0,
            recentSamples = emptyList(),
            learningEnabled = false
        )

        // Learned prediction should be shifted toward user's actual 38-40 minutes
        assertTrue("Learned delay (${learnedResult.predictedDelayMinutes}) should be closer to user average than default (${unlearnedResult.predictedDelayMinutes})",
            learnedResult.predictedDelayMinutes <= unlearnedResult.predictedDelayMinutes)
    }

    @Test
    fun testLocalParserBathroom() {
        val res1 = LocalParser.parse("دخلت الحمام دلوقتي")
        assertEquals(EventType.BATHROOM, res1.type)

        val res2 = LocalParser.parse("حمام")
        assertEquals(EventType.BATHROOM, res2.type)

        val res3 = LocalParser.parse("toilet")
        assertEquals(EventType.BATHROOM, res3.type)
    }
}
