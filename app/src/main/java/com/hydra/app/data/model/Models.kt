package com.hydra.app.data.model

enum class EventType {
    WATER,
    FOOD,
    CREATINE,
    BATHROOM,
    NOTE
}

enum class BathroomUrgeType {
    URINATION,   // تبول وترطيب
    DIGESTION,   // هضم وسعرات (منعكس معدي قولوني)
    COMBINED     // ترطيب وهضم متكامل
}

enum class EventSource {
    MANUAL,
    AI,
    QUICK_ACTION,
    VOICE
}

data class FoodNutrients(
    val calories: Int? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null
)

data class ParsedInput(
    val type: EventType,
    val amount: Double? = null,
    val unit: String? = null,
    val description: String? = null,
    val nutrients: FoodNutrients? = null,
    val confidence: Double = 1.0,
    val source: EventSource = EventSource.MANUAL,
    val rawText: String = ""
)

enum class AiProvider {
    OFF,
    GEMINI
}
