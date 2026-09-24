package com.hydra.app

import com.hydra.app.data.local.PredictionSampleEntity
import com.hydra.app.domain.prediction.PredictionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionEngineTest {

    @Test
    fun testPredictionWithNoHistory() {
        val delay = PredictionEngine.calculatePredictedDelay(
            waterAmountMl = 500,
            recentSamples = emptyList(),
            minDelayMinutes = 30,
            maxDelayMinutes = 120
        )
        // For 500ml, delay is ~50 minutes
        assertTrue("Delay for 500ml should be in 45..55 range", delay in 45..55)
    }

    @Test
    fun testPredictionWithWaterVolumeScaling() {
        val delay250 = PredictionEngine.calculateBaseline(250)
        val delay500 = PredictionEngine.calculateBaseline(500)
        val delay1000 = PredictionEngine.calculateBaseline(1000)

        // Physiologically, acute high water bolus causes faster diuresis
        assertTrue("250ml delay should be greater than or equal to 500ml ($delay250 >= $delay500)", delay250 >= delay500)
        assertTrue("500ml delay should be greater than or equal to 1000ml ($delay500 >= $delay1000)", delay500 >= delay1000)
    }

    @Test
    fun testPredictionWithPersonalHistory() {
        // User consistently responds after 38 minutes
        val samples = List(15) { index ->
            PredictionSampleEntity(
                id = index.toLong(),
                waterAmount = 500,
                caloriesAmount = 0,
                predictedDelayMinutes = 55,
                actualDelayMinutes = 38,
                createdAt = System.currentTimeMillis() - (index * 3600000L),
                feedbackGiven = true
            )
        }

        val predicted = PredictionEngine.calculatePredictedDelay(
            waterAmountMl = 500,
            recentSamples = samples,
            minDelayMinutes = 30,
            maxDelayMinutes = 120,
            learningEnabled = true
        )

        // Should adapt closer to 38 than original baseline 50
        assertTrue("Predicted delay ($predicted) should be less than 50", predicted < 50)
        assertTrue("Predicted delay ($predicted) should be at least 38", predicted >= 38)
    }

    @Test
    fun testBoundsEnforcement() {
        val delayClampedMin = PredictionEngine.calculatePredictedDelay(
            waterAmountMl = 1000,
            recentSamples = emptyList(),
            minDelayMinutes = 70, // Min enforced
            maxDelayMinutes = 120
        )
        assertEquals(70, delayClampedMin)

        val delayClampedMax = PredictionEngine.calculatePredictedDelay(
            waterAmountMl = 100, // Normally ~68 min
            recentSamples = emptyList(),
            minDelayMinutes = 20,
            maxDelayMinutes = 45 // Max enforced
        )
        assertEquals(45, delayClampedMax)
    }
}
