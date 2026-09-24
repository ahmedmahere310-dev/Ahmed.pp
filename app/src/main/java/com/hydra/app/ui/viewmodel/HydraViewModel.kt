package com.hydra.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hydra.app.data.local.EventEntity
import com.hydra.app.data.local.HydraUserPreferences
import com.hydra.app.data.local.PredictionSampleEntity
import com.hydra.app.data.local.PreferencesManager
import com.hydra.app.data.local.SecureKeyStorage
import com.hydra.app.data.model.AiProvider
import com.hydra.app.data.model.EventSource
import com.hydra.app.data.model.EventType
import com.hydra.app.data.model.ParsedInput
import com.hydra.app.data.repository.EventRepository
import com.hydra.app.data.repository.PredictionRepository
import com.hydra.app.domain.ai.AIService
import com.hydra.app.domain.parser.InputParser
import com.hydra.app.domain.prediction.PredictionEngine
import com.hydra.app.notification.NotificationHelper
import com.hydra.app.notification.ReminderAlarmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class HydraUiState(
    val selectedTab: Int = 0, // 0: Home, 1: History, 2: Statistics, 3: Settings
    val todayWaterMl: Int = 0,
    val todayFoodCount: Int = 0,
    val todayCalories: Int = 0,
    val todayBathroomCount: Int = 0,
    val todayCreatineTaken: Boolean = false,
    val todayCreatineAmount: Double? = null,
    val todayEvents: List<EventEntity> = emptyList(),
    val allEvents: List<EventEntity> = emptyList(),
    val predictionSamples: List<PredictionSampleEntity> = emptyList(),
    val activeReminderSample: PredictionSampleEntity? = null,
    val userPreferences: HydraUserPreferences = HydraUserPreferences(),
    val hasApiKey: Boolean = false,
    val isTestingApi: Boolean = false,
    val apiTestResult: String? = null,
    val isProcessingInput: Boolean = false,
    val inputMessage: String = "",
    val snackbarMessage: String? = null,
    val showFeedbackDialog: Boolean = false,
    val nextWaterReminderText: String? = null,
    val nextFoodReminderText: String? = null
)

class HydraViewModel(
    private val context: Context,
    private val eventRepository: EventRepository,
    private val predictionRepository: PredictionRepository,
    private val preferencesManager: PreferencesManager,
    private val secureKeyStorage: SecureKeyStorage,
    private val reminderAlarmManager: ReminderAlarmManager,
    private val inputParser: InputParser,
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HydraUiState())
    val uiState: StateFlow<HydraUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val (startOfDay, endOfDay) = getTodayRange()

        viewModelScope.launch {
            combine(
                eventRepository.getEventsForDay(startOfDay, endOfDay),
                eventRepository.getEvents(),
                predictionRepository.getAllSamples(),
                preferencesManager.userPreferencesFlow
            ) { todayEvents, allEvents, samples, prefs ->
                val waterMl = todayEvents.filter { it.type == EventType.WATER.name }
                    .sumOf { it.amount?.toInt() ?: 0 }
                val foodCount = todayEvents.count { it.type == EventType.FOOD.name }
                val totalCalories = todayEvents.filter { it.type == EventType.FOOD.name }
                    .sumOf { it.calories ?: 0 }
                val bathroomCount = todayEvents.count { it.type == EventType.BATHROOM.name }
                val creatineEvent = todayEvents.firstOrNull { it.type == EventType.CREATINE.name }
                val activeSample = samples.firstOrNull { !it.feedbackGiven }
                val hasKey = secureKeyStorage.getGeminiApiKey().isNotBlank()

                val nextWaterText = if (prefs.periodicWaterEnabled) {
                    formatTimeRemaining(prefs.lastWaterLoggedTime, prefs.waterIntervalMinutes)
                } else null

                val nextFoodText = if (prefs.periodicFoodEnabled) {
                    formatTimeRemaining(prefs.lastFoodLoggedTime, prefs.foodIntervalMinutes)
                } else null

                _uiState.update { current ->
                    current.copy(
                        todayWaterMl = waterMl,
                        todayFoodCount = foodCount,
                        todayCalories = totalCalories,
                        todayBathroomCount = bathroomCount,
                        todayCreatineTaken = creatineEvent != null,
                        todayCreatineAmount = creatineEvent?.amount,
                        todayEvents = todayEvents,
                        allEvents = allEvents,
                        predictionSamples = samples,
                        activeReminderSample = activeSample,
                        userPreferences = prefs,
                        hasApiKey = hasKey,
                        nextWaterReminderText = nextWaterText,
                        nextFoodReminderText = nextFoodText
                    )
                }
            }.stateIn(viewModelScope)
        }
    }

    private fun formatTimeRemaining(lastLoggedTime: Long, intervalMinutes: Int): String {
        if (lastLoggedTime == 0L) return "كل $intervalMinutes دقيقة"
        val now = System.currentTimeMillis()
        val elapsedMin = ((now - lastLoggedTime) / (60 * 1000)).toInt()
        val remaining = intervalMinutes - elapsedMin
        return when {
            remaining <= 0 -> "قريباً"
            remaining < 60 -> "بعد $remaining د"
            else -> "بعد ${remaining / 60} س ${if (remaining % 60 > 0) "و ${remaining % 60} د" else ""}"
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun submitNaturalInput(text: String, source: EventSource = EventSource.MANUAL) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _uiState.update { it.copy(isProcessingInput = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = secureKeyStorage.getGeminiApiKey()
                val prefs = _uiState.value.userPreferences

                val parsed = inputParser.parse(
                    input = trimmed,
                    source = source,
                    aiProvider = prefs.aiProvider,
                    apiKey = apiKey
                )

                saveParsedEvent(parsed)

                _uiState.update {
                    it.copy(
                        isProcessingInput = false,
                        inputMessage = "",
                        snackbarMessage = "تم تسجيل: ${getEventSummary(parsed)}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingInput = false,
                        snackbarMessage = "حدث خطأ أثناء المعالجة: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun logQuickWater(amountMl: Int) {
        val parsed = ParsedInput(
            type = EventType.WATER,
            amount = amountMl.toDouble(),
            unit = "ml",
            description = "ماء",
            confidence = 1.0,
            source = EventSource.QUICK_ACTION,
            rawText = "+$amountMl ml"
        )
        viewModelScope.launch(Dispatchers.IO) {
            saveParsedEvent(parsed)
            _uiState.update { it.copy(snackbarMessage = "تم تسجيل $amountMl مل ماء") }
        }
    }

    fun logQuickCreatine(amountG: Double = 5.0) {
        val parsed = ParsedInput(
            type = EventType.CREATINE,
            amount = amountG,
            unit = "g",
            description = "كرياتين",
            confidence = 1.0,
            source = EventSource.QUICK_ACTION,
            rawText = "جرعة كرياتين"
        )
        viewModelScope.launch(Dispatchers.IO) {
            saveParsedEvent(parsed)
            _uiState.update { it.copy(snackbarMessage = "تم تسجيل جرعة الكرياتين ($amountG جم)") }
        }
    }

    fun logQuickFood(description: String) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return
        val parsed = ParsedInput(
            type = EventType.FOOD,
            amount = null,
            unit = null,
            description = trimmed,
            confidence = 1.0,
            source = EventSource.QUICK_ACTION,
            rawText = trimmed
        )
        viewModelScope.launch(Dispatchers.IO) {
            saveParsedEvent(parsed)
            _uiState.update { it.copy(snackbarMessage = "تم تسجيل وجبة: $trimmed") }
        }
    }

    fun logQuickBathroom() {
        val parsed = ParsedInput(
            type = EventType.BATHROOM,
            description = "دخول الحمام",
            confidence = 1.0,
            source = EventSource.QUICK_ACTION,
            rawText = "دخول الحمام"
        )
        viewModelScope.launch(Dispatchers.IO) {
            saveParsedEvent(parsed)
            _uiState.update { it.copy(snackbarMessage = "تم تسجيل دخول الحمام وتحديث خوارزمية التعلم الذاتي 🚻") }
        }
    }

    private suspend fun saveParsedEvent(parsed: ParsedInput) {
        val eventEntity = EventEntity(
            type = parsed.type.name,
            createdAt = System.currentTimeMillis(),
            amount = parsed.amount,
            unit = parsed.unit,
            description = parsed.description,
            calories = parsed.nutrients?.calories,
            protein = parsed.nutrients?.protein,
            carbs = parsed.nutrients?.carbs,
            fat = parsed.nutrients?.fat,
            source = parsed.source.name,
            confidence = parsed.confidence
        )

        eventRepository.insertEvent(eventEntity)

        val prefs = _uiState.value.userPreferences

        // 1. Water event handling
        if (parsed.type == EventType.WATER && parsed.amount != null && parsed.amount > 0) {
            preferencesManager.recordWaterLogged(System.currentTimeMillis())

            // Reschedule periodic water reminder
            if (prefs.periodicWaterEnabled) {
                reminderAlarmManager.scheduleNextWaterReminder(
                    intervalMinutes = prefs.waterIntervalMinutes,
                    quietHoursStart = prefs.quietHoursStart,
                    quietHoursEnd = prefs.quietHoursEnd
                )
            }

            // Smart Bathroom prediction based on water + recent calories
            if (prefs.remindersEnabled) {
                scheduleBathroomPrediction(waterMl = parsed.amount.toInt(), calories = 0, prefs = prefs)
            }
        }

        // 2. Food event handling
        if (parsed.type == EventType.FOOD) {
            preferencesManager.recordFoodLogged(System.currentTimeMillis())

            // Reschedule periodic food reminder
            if (prefs.periodicFoodEnabled) {
                reminderAlarmManager.scheduleNextFoodReminder(
                    intervalMinutes = prefs.foodIntervalMinutes,
                    quietHoursStart = prefs.quietHoursStart,
                    quietHoursEnd = prefs.quietHoursEnd
                )
            }

            // Smart Bathroom prediction based on calories + recent water
            if (prefs.remindersEnabled) {
                val calories = parsed.nutrients?.calories ?: 350
                scheduleBathroomPrediction(waterMl = 0, calories = calories, prefs = prefs)
            }
        }

        // 3. Bathroom event handling: resolves active prediction sample for self-learning
        if (parsed.type == EventType.BATHROOM) {
            resolveActiveSampleOnBathroomVisit()
        }
    }

    private suspend fun resolveActiveSampleOnBathroomVisit() {
        val now = System.currentTimeMillis()
        val samples = predictionRepository.getAllSamplesList()
        val active = samples.firstOrNull { !it.feedbackGiven }
        if (active != null) {
            val elapsedMinutes = ((now - active.createdAt) / (60 * 1000)).toInt().coerceAtLeast(1)
            val updated = active.copy(
                actualDelayMinutes = elapsedMinutes,
                feedbackGiven = true
            )
            predictionRepository.updateSample(updated)
            reminderAlarmManager.cancelReminder()
        }
    }

    private suspend fun scheduleBathroomPrediction(
        waterMl: Int,
        calories: Int,
        prefs: HydraUserPreferences
    ) {
        val now = System.currentTimeMillis()
        val allEvents = eventRepository.getAllEventsList()

        // 1. Calculate recent water in last 2 hours
        val twoHoursAgo = now - (2 * 60 * 60 * 1000L)
        val recentWaterEvents = allEvents.filter {
            it.type == EventType.WATER.name && it.createdAt >= twoHoursAgo
        }
        val effectiveWater = if (waterMl > 0) {
            waterMl + recentWaterEvents.filter { it.createdAt < now - 30000 }.sumOf { it.amount?.toInt() ?: 0 }
        } else {
            recentWaterEvents.sumOf { it.amount?.toInt() ?: 0 }
        }

        // 2. Calculate recent calories in last 3 hours
        val threeHoursAgo = now - (3 * 60 * 60 * 1000L)
        val recentFoodEvents = allEvents.filter {
            it.type == EventType.FOOD.name && it.createdAt >= threeHoursAgo
        }
        val effectiveCalories = if (calories > 0) {
            calories + recentFoodEvents.filter { it.createdAt < now - 30000 }.sumOf { it.calories ?: 350 }
        } else {
            recentFoodEvents.sumOf { it.calories ?: 350 }
        }

        // 3. Time since last bathroom visit
        val lastBathroom = allEvents.firstOrNull { it.type == EventType.BATHROOM.name }
        val minutesSinceBathroom = lastBathroom?.let {
            ((now - it.createdAt) / (60 * 1000)).toInt().coerceAtLeast(0)
        }

        // 4. Run prediction engine
        val recentSamples = predictionRepository.getRecentCompletedSamplesList(20)
        val prediction = PredictionEngine.calculateBathroomPrediction(
            waterAmountMl = effectiveWater,
            calories = effectiveCalories,
            recentSamples = recentSamples,
            minutesSinceLastBathroom = minutesSinceBathroom,
            minDelayMinutes = prefs.minDelayMinutes,
            maxDelayMinutes = prefs.maxDelayMinutes,
            learningEnabled = prefs.learningEnabled
        )

        // 5. Save sample to database & schedule alarm
        val sample = PredictionSampleEntity(
            waterAmount = effectiveWater,
            caloriesAmount = effectiveCalories,
            urgeType = prediction.urgeType.name,
            explanation = prediction.explanationArabic,
            predictedDelayMinutes = prediction.predictedDelayMinutes,
            actualDelayMinutes = null,
            createdAt = now,
            feedbackGiven = false
        )

        val sampleId = predictionRepository.insertSample(sample)
        reminderAlarmManager.scheduleReminder(sampleId, prediction.predictedDelayMinutes)
    }

    fun testBathroomReminder() {
        val sampleId = System.currentTimeMillis()
        val water = if (_uiState.value.todayWaterMl > 0) _uiState.value.todayWaterMl else 400
        val cal = if (_uiState.value.todayCalories > 0) _uiState.value.todayCalories else 550
        NotificationHelper.showBathroomReminderNotification(
            context = context,
            sampleId = sampleId,
            waterMl = water,
            calories = cal,
            urgeType = "COMBINED"
        )
        _uiState.update { it.copy(snackbarMessage = "تم إرسال إشعار تجريبي لدخول الحمام 🚻") }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            eventRepository.deleteEvent(event)
            _uiState.update { it.copy(snackbarMessage = "تم حذف الحدث") }
        }
    }

    fun recordFeedback(sample: PredictionSampleEntity, wasNow: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val elapsedMinutes = ((now - sample.createdAt) / (60 * 1000)).toInt().coerceAtLeast(1)

            val updated = if (wasNow) {
                // Also record bathroom event
                val event = EventEntity(
                    type = EventType.BATHROOM.name,
                    createdAt = now,
                    description = "دخول الحمام (تأكيد التذكير)",
                    source = EventSource.QUICK_ACTION.name,
                    confidence = 1.0
                )
                eventRepository.insertEvent(event)

                sample.copy(
                    actualDelayMinutes = elapsedMinutes,
                    feedbackGiven = true
                )
            } else {
                // User pressed "Not yet" (لسه) -> reschedule for snooze minutes
                reminderAlarmManager.scheduleReminder(sample.id, _uiState.value.userPreferences.snoozeMinutes)
                sample
            }

            if (wasNow) {
                predictionRepository.updateSample(updated)
                _uiState.update {
                    it.copy(
                        snackbarMessage = "شكراً لمشاركتك! تم تحديث خوارزمية التعلم الذاتي لدخول الحمام.",
                        showFeedbackDialog = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        snackbarMessage = "تم تأجيل التذكير ${_uiState.value.userPreferences.snoozeMinutes} دقيقة.",
                        showFeedbackDialog = false
                    )
                }
            }
        }
    }

    fun dismissFeedbackDialog() {
        _uiState.update { it.copy(showFeedbackDialog = false) }
    }

    fun showFeedbackDialog() {
        _uiState.update { it.copy(showFeedbackDialog = true) }
    }

    // --- Periodic Reminders Controls ---
    fun setPeriodicWaterReminder(enabled: Boolean, intervalMinutes: Int? = null) {
        viewModelScope.launch {
            preferencesManager.setPeriodicWaterReminder(enabled, intervalMinutes)
            val prefs = _uiState.value.userPreferences
            val interval = intervalMinutes ?: prefs.waterIntervalMinutes
            if (enabled) {
                reminderAlarmManager.scheduleNextWaterReminder(
                    intervalMinutes = interval,
                    quietHoursStart = prefs.quietHoursStart,
                    quietHoursEnd = prefs.quietHoursEnd
                )
                _uiState.update { it.copy(snackbarMessage = "تم تفعيل تذكير شرب الماء (كل $interval دقيقة)") }
            } else {
                reminderAlarmManager.cancelWaterReminder()
                _uiState.update { it.copy(snackbarMessage = "تم إيقاف تذكير شرب الماء الدوري") }
            }
        }
    }

    fun setPeriodicFoodReminder(enabled: Boolean, intervalMinutes: Int? = null) {
        viewModelScope.launch {
            preferencesManager.setPeriodicFoodReminder(enabled, intervalMinutes)
            val prefs = _uiState.value.userPreferences
            val interval = intervalMinutes ?: prefs.foodIntervalMinutes
            if (enabled) {
                reminderAlarmManager.scheduleNextFoodReminder(
                    intervalMinutes = interval,
                    quietHoursStart = prefs.quietHoursStart,
                    quietHoursEnd = prefs.quietHoursEnd
                )
                val hours = interval / 60
                _uiState.update { it.copy(snackbarMessage = "تم تفعيل تذكير الوجبات (كل $hours ساعات)") }
            } else {
                reminderAlarmManager.cancelFoodReminder()
                _uiState.update { it.copy(snackbarMessage = "تم إيقاف تذكير الوجبات الدوري") }
            }
        }
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            preferencesManager.setQuietHours(startHour, endHour)
            _uiState.update { it.copy(snackbarMessage = "تم تحديث ساعات الهدوء ($startHour:00 حتى $endHour:00)") }
        }
    }

    fun testPeriodicWaterReminder() {
        NotificationHelper.showPeriodicWaterNotification(
            context = context,
            todayWaterMl = _uiState.value.todayWaterMl,
            goalMl = _uiState.value.userPreferences.dailyWaterGoalMl
        )
        _uiState.update { it.copy(snackbarMessage = "تم إرسال إشعار تجريبي لشرب الماء 💧") }
    }

    fun testPeriodicFoodReminder() {
        NotificationHelper.showPeriodicFoodNotification(
            context = context,
            todayFoodCount = _uiState.value.todayFoodCount
        )
        _uiState.update { it.copy(snackbarMessage = "تم إرسال إشعار تجريبي للوجبات 🍽️") }
    }

    // --- AI & General Settings ---
    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            preferencesManager.setAiProvider(provider)
        }
    }

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            secureKeyStorage.saveGeminiApiKey(key)
            _uiState.update {
                it.copy(
                    hasApiKey = key.isNotBlank(),
                    snackbarMessage = "تم حفظ مفتاح API بأمان في Android Keystore"
                )
            }
        }
    }

    fun testGeminiConnection() {
        val key = secureKeyStorage.getGeminiApiKey()
        if (key.isBlank()) {
            _uiState.update { it.copy(apiTestResult = "يرجى إدخال مفتاح API أولاً") }
            return
        }

        _uiState.update { it.copy(isTestingApi = true, apiTestResult = null) }

        viewModelScope.launch {
            val result = aiService.testConnection(key)
            _uiState.update {
                it.copy(
                    isTestingApi = false,
                    apiTestResult = if (result.isSuccess) "اتصال ناجح! مفتاح Gemini يعمل بشكل سليم." else "فشل الاتصال: ${result.exceptionOrNull()?.localizedMessage}"
                )
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setRemindersEnabled(enabled)
            if (!enabled) {
                reminderAlarmManager.cancelReminder()
            }
        }
    }

    fun setDelayBounds(min: Int, max: Int) {
        viewModelScope.launch {
            preferencesManager.setDelayBounds(min, max)
        }
    }

    fun setLearningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setLearningEnabled(enabled)
        }
    }

    fun setDailyWaterGoal(goalMl: Int) {
        viewModelScope.launch {
            preferencesManager.setDailyWaterGoal(goalMl)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            eventRepository.clearAllEvents()
            predictionRepository.clearAllSamples()
            reminderAlarmManager.cancelReminder()
            reminderAlarmManager.cancelWaterReminder()
            reminderAlarmManager.cancelFoodReminder()
            _uiState.update { it.copy(snackbarMessage = "تم مسح كافة البيانات المسجلة بنجاح") }
        }
    }

    suspend fun exportDataJson(): String {
        val events = eventRepository.getAllEventsList()
        val samples = predictionRepository.getAllSamplesList()

        val root = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("app", "Hydra Life Tracker")
            put("version", "1.0")

            val eventsArray = JSONArray()
            events.forEach { e ->
                val obj = JSONObject().apply {
                    put("id", e.id)
                    put("type", e.type)
                    put("createdAt", e.createdAt)
                    put("amount", e.amount)
                    put("unit", e.unit)
                    put("description", e.description)
                    put("calories", e.calories)
                    put("protein", e.protein)
                    put("carbs", e.carbs)
                    put("fat", e.fat)
                    put("source", e.source)
                    put("confidence", e.confidence)
                }
                eventsArray.put(obj)
            }
            put("events", eventsArray)

            val samplesArray = JSONArray()
            samples.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("waterAmount", s.waterAmount)
                    put("predictedDelayMinutes", s.predictedDelayMinutes)
                    put("actualDelayMinutes", s.actualDelayMinutes)
                    put("createdAt", s.createdAt)
                    put("feedbackGiven", s.feedbackGiven)
                }
                samplesArray.put(obj)
            }
            put("samples", samplesArray)
        }

        return root.toString(2)
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun getEventSummary(parsed: ParsedInput): String {
        return when (parsed.type) {
            EventType.WATER -> "${parsed.amount?.toInt() ?: 0} مل ماء"
            EventType.CREATINE -> "${parsed.amount ?: 5.0} جم كرياتين"
            EventType.FOOD -> parsed.description ?: "وجبة طعام"
            EventType.BATHROOM -> "دخول الحمام 🚻"
            EventType.NOTE -> parsed.description ?: "ملاحظة"
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        return Pair(startOfDay, endOfDay)
    }
}

class HydraViewModelFactory(
    private val app: com.hydra.app.HydraApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HydraViewModel::class.java)) {
            return HydraViewModel(
                context = app.applicationContext,
                eventRepository = app.eventRepository,
                predictionRepository = app.predictionRepository,
                preferencesManager = app.preferencesManager,
                secureKeyStorage = app.secureKeyStorage,
                reminderAlarmManager = app.reminderAlarmManager,
                inputParser = app.inputParser,
                aiService = app.geminiAiService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
