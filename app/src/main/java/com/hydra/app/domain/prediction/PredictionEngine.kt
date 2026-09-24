package com.hydra.app.domain.prediction

import com.hydra.app.data.local.PredictionSampleEntity
import com.hydra.app.data.model.BathroomUrgeType
import kotlin.math.abs
import kotlin.math.roundToInt

data class BathroomPredictionResult(
    val predictedDelayMinutes: Int,
    val urgeType: BathroomUrgeType,
    val waterAmountMl: Int,
    val caloriesAmount: Int,
    val explanationArabic: String,
    val diuresisDelayMinutes: Int,
    val gastrocolicDelayMinutes: Int,
    val confidenceScore: Float = 0.9f
)

/**
 * خوارزمية ذكية متقدمة لتقدير وقت الحاجة لدخول الحمام (تبول / هضم وإخراج).
 * تحسب معاً:
 * 1. كمية السوائل/الماء المتناولة وتأثيرها على إدرار البول وسرعة امتلاء المثانة (Diuresis Latency).
 * 2. السعرات الحرارية للوجبات وتأثيرها على المنعكس المعدي القولوني (Gastrocolic Reflex) وحركة الجهاز الهضمي.
 * 3. التفاعل الفسيولوجي المشترك بين الطعام والماء (Gastric Emptying & Buffer Effect).
 * 4. التعلم الذاتي التكيفي من استجابات المستخدم السابقة (Adaptive Personal Feedback ML).
 */
object PredictionEngine {

    /**
     * الحساب الفسيولوجي الشامل لدخول الحمام بالاعتماد على الماء والسعرات والوقت.
     */
    fun calculateBathroomPrediction(
        waterAmountMl: Int,
        calories: Int,
        recentSamples: List<PredictionSampleEntity> = emptyList(),
        minutesSinceLastBathroom: Int? = null,
        minDelayMinutes: Int = 20,
        maxDelayMinutes: Int = 150,
        learningEnabled: Boolean = true
    ): BathroomPredictionResult {
        val safeWater = waterAmountMl.coerceAtLeast(0)
        val safeCalories = calories.coerceAtLeast(0)

        // 1. حساب زمن إدرار البول من الماء (Diuresis Delay)
        val diuresisDelay = calculateDiuresisDelay(safeWater, minutesSinceLastBathroom)

        // 2. حساب زمن تنشيط الهضم من السعرات (Gastrocolic Reflex Delay)
        val gastrocolicDelay = calculateGastrocolicDelay(safeCalories)

        // 3. تحديد نوع الحاجة المحفزة (Urge Type) والزمن الفسيولوجي الأولي
        val hasSignificantWater = safeWater >= 120
        val hasSignificantFood = safeCalories >= 150

        val (rawDelayMinutes, urgeType) = when {
            hasSignificantWater && hasSignificantFood -> {
                // عند اجتماع الماء مع الطعام:
                // الطعام الصلب يبطئ تفريغ السوائل من المعدة قليلاً (+8 دقائق)،
                // وفي نفس الوقت السعرات تنشط القولون. نأخذ الأسرع وصولاً لقمة التحفيز:
                val bufferedWaterDelay = diuresisDelay + 8
                val combined = minOf(bufferedWaterDelay, gastrocolicDelay)
                Pair(combined, BathroomUrgeType.COMBINED)
            }
            hasSignificantWater -> {
                Pair(diuresisDelay, BathroomUrgeType.URINATION)
            }
            hasSignificantFood -> {
                Pair(gastrocolicDelay, BathroomUrgeType.DIGESTION)
            }
            else -> {
                // مدخلات خفيفة جداً (أقل من كوب ماء وسناك خفيف)
                Pair(diuresisDelay.coerceAtMost(80), BathroomUrgeType.URINATION)
            }
        }

        // 4. دمج التعلم الذاتي الشخصي (Adaptive Personal Learning)
        val finalDelayMinutes = applyPersonalLearning(
            rawBaselineMinutes = rawDelayMinutes,
            targetWater = safeWater,
            targetCalories = safeCalories,
            recentSamples = recentSamples,
            minDelay = minDelayMinutes,
            maxDelay = maxDelayMinutes,
            learningEnabled = learningEnabled
        )

        // 5. صياغة التفسير التوضيحي باللغة العربية للمستخدم
        val explanation = buildArabicExplanation(
            waterMl = safeWater,
            calories = safeCalories,
            delayMinutes = finalDelayMinutes,
            urgeType = urgeType
        )

        return BathroomPredictionResult(
            predictedDelayMinutes = finalDelayMinutes,
            urgeType = urgeType,
            waterAmountMl = safeWater,
            caloriesAmount = safeCalories,
            explanationArabic = explanation,
            diuresisDelayMinutes = diuresisDelay,
            gastrocolicDelayMinutes = gastrocolicDelay
        )
    }

    /**
     * حساب إدرار البول الفسيولوجي:
     * - كمية 200-250 مل: ~60-70 دقيقة
     * - كمية 500 مل: ~50-55 دقيقة
     * - كمية 750-1000 مل+: ~35-45 دقيقة (تثبيط أسرع لهرمون ADH)
     * - إذا مر وقت طويل على آخر حمام: المثانة ممتلئة جزئياً مسبقاً، فيقل الوقت.
     */
    fun calculateDiuresisDelay(waterAmountMl: Int, minutesSinceLastBathroom: Int? = null): Int {
        if (waterAmountMl <= 0) return 90

        val clampedWater = waterAmountMl.coerceIn(100, 2000)
        // الأساس الفسيولوجي: الماء الأكبر يفرز بحدة وسرعة أكبر
        val baseDelay = when {
            clampedWater <= 250 -> 68 - ((clampedWater - 100) / 150.0 * 8).roundToInt()
            clampedWater <= 500 -> 60 - ((clampedWater - 250) / 250.0 * 10).roundToInt()
            clampedWater <= 800 -> 50 - ((clampedWater - 500) / 300.0 * 8).roundToInt()
            else -> 42 - ((clampedWater - 800) / 1200.0 * 7).roundToInt()
        }

        // خصم تراكم البول الأساسي إذا كان المستخدم لم يدخل الحمام منذ فترة
        val adjustedForPreFill = if (minutesSinceLastBathroom != null && minutesSinceLastBathroom > 45) {
            val discount = ((minutesSinceLastBathroom - 45) / 15).coerceIn(0, 15)
            baseDelay - discount
        } else {
            baseDelay
        }

        return adjustedForPreFill.coerceIn(25, 95)
    }

    /**
     * حساب المنعكس المعدي القولوني (Gastrocolic Reflex) بناءً على السعرات:
     * - وجبة خفيفة (<200 سعرة): تحفيز بطيء ~75-90 دقيقة
     * - وجبة متوسطة (250-550 سعرة): تحفيز ملحوظ ~40-55 دقيقة
     * - وجبة دسمة/كبيرة (>600-1000+ سعرة): تحفيز سريع وقوي خلال 25-40 دقيقة
     */
    fun calculateGastrocolicDelay(calories: Int): Int {
        if (calories <= 0) return 100

        val clampedCal = calories.coerceIn(50, 1500)
        return when {
            clampedCal < 200 -> 85 - ((clampedCal - 50) / 150.0 * 15).roundToInt()
            clampedCal <= 500 -> 70 - ((clampedCal - 200) / 300.0 * 25).roundToInt()
            clampedCal <= 800 -> 45 - ((clampedCal - 500) / 300.0 * 12).roundToInt()
            else -> 33 - ((clampedCal - 800) / 700.0 * 8).roundToInt()
        }.coerceIn(25, 110)
    }

    /**
     * دمج السجل الشخصي التكيفي مع الأساس الحسابي
     */
    private fun applyPersonalLearning(
        rawBaselineMinutes: Int,
        targetWater: Int,
        targetCalories: Int,
        recentSamples: List<PredictionSampleEntity>,
        minDelay: Int,
        maxDelay: Int,
        learningEnabled: Boolean
    ): Int {
        val completedSamples = recentSamples.filter { it.feedbackGiven && it.actualDelayMinutes != null }
        if (!learningEnabled || completedSamples.isEmpty()) {
            return rawBaselineMinutes.coerceIn(minDelay, maxDelay)
        }

        var weightedSum = 0.0
        var totalWeight = 0.0

        completedSamples.take(20).forEachIndexed { index, sample ->
            val actual = sample.actualDelayMinutes?.toDouble() ?: return@forEachIndexed

            // وزن الحداثة: الأحدث يحظى بأولوية أكبر
            val recencyWeight = 1.0 - (index * 0.03).coerceAtMost(0.4)

            // وزن التشابه في كمية الماء
            val waterDiff = abs(sample.waterAmount - targetWater)
            val waterSimilarity = when {
                waterDiff <= 100 -> 1.3
                waterDiff <= 250 -> 1.0
                else -> 0.7
            }

            // وزن التشابه في السعرات
            val calDiff = abs(sample.caloriesAmount - targetCalories)
            val calSimilarity = when {
                calDiff <= 100 -> 1.3
                calDiff <= 250 -> 1.0
                else -> 0.8
            }

            val sampleWeight = recencyWeight * waterSimilarity * calSimilarity
            weightedSum += actual * sampleWeight
            totalWeight += sampleWeight
        }

        val personalAverage = if (totalWeight > 0) weightedSum / totalWeight else rawBaselineMinutes.toDouble()

        // نسبة الاعتماد على السجل الشخصي ترتفع مع ازدياد العينات حتى 75%
        val personalRatio = (completedSamples.size / 15.0).coerceIn(0.2, 0.75)
        val baselineRatio = 1.0 - personalRatio

        val blended = (personalRatio * personalAverage) + (baselineRatio * rawBaselineMinutes)
        return blended.roundToInt().coerceIn(minDelay, maxDelay)
    }

    private fun buildArabicExplanation(
        waterMl: Int,
        calories: Int,
        delayMinutes: Int,
        urgeType: BathroomUrgeType
    ): String {
        return when (urgeType) {
            BathroomUrgeType.COMBINED -> {
                "حساب تقديري: شرب $waterMl مل ماء مع وجبة $calories سعرة. يُتوقع امتلاء المثانة ونشاط الهضم خلال حوالي $delayMinutes دقيقة."
            }
            BathroomUrgeType.URINATION -> {
                "حساب تقديري: شرب $waterMl مل ماء. بناءً على معدل إدرار البول والترطيب، يُتوقع امتلاء المثانة خلال $delayMinutes دقيقة."
            }
            BathroomUrgeType.DIGESTION -> {
                "حساب تقديري: وجبة $calories سعرة حرارية. يُتوقع تنشيط المنعكس القولوني وحركة الأمعاء خلال $delayMinutes دقيقة."
            }
        }
    }

    /**
     * دالة التوافقية السابقة لدعم النداءات المباشرة للشرب فقط
     */
    fun calculatePredictedDelay(
        waterAmountMl: Int,
        recentSamples: List<PredictionSampleEntity>,
        minDelayMinutes: Int = 30,
        maxDelayMinutes: Int = 120,
        learningEnabled: Boolean = true
    ): Int {
        val result = calculateBathroomPrediction(
            waterAmountMl = waterAmountMl,
            calories = 0,
            recentSamples = recentSamples,
            minDelayMinutes = minDelayMinutes,
            maxDelayMinutes = maxDelayMinutes,
            learningEnabled = learningEnabled
        )
        return result.predictedDelayMinutes
    }

    /**
     * دالة الأساس النظري للماء فقط (للتوافق القديم)
     */
    fun calculateBaseline(waterAmountMl: Int): Int {
        return calculateDiuresisDelay(waterAmountMl)
    }
}
