package com.hydra.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // WATER, FOOD, CREATINE, NOTE
    val createdAt: Long = System.currentTimeMillis(),
    val amount: Double? = null,
    val unit: String? = null,
    val description: String? = null,
    val calories: Int? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val source: String = "MANUAL", // MANUAL, AI, QUICK_ACTION, VOICE
    val confidence: Double = 1.0
)

@Entity(tableName = "prediction_samples")
data class PredictionSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val waterAmount: Int = 0,
    val caloriesAmount: Int = 0,
    val urgeType: String = "COMBINED",
    val explanation: String = "",
    val predictedDelayMinutes: Int,
    val actualDelayMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val feedbackGiven: Boolean = false
)
