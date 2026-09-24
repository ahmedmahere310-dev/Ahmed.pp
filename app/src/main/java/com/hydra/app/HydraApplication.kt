package com.hydra.app

import android.app.Application
import com.hydra.app.data.local.AppDatabase
import com.hydra.app.data.local.PreferencesManager
import com.hydra.app.data.local.SecureKeyStorage
import com.hydra.app.data.repository.EventRepository
import com.hydra.app.data.repository.PredictionRepository
import com.hydra.app.domain.ai.GeminiAIService
import com.hydra.app.domain.parser.InputParser
import com.hydra.app.notification.NotificationHelper
import com.hydra.app.notification.ReminderAlarmManager

class HydraApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var eventRepository: EventRepository
        private set

    lateinit var predictionRepository: PredictionRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var secureKeyStorage: SecureKeyStorage
        private set

    lateinit var reminderAlarmManager: ReminderAlarmManager
        private set

    lateinit var geminiAiService: GeminiAIService
        private set

    lateinit var inputParser: InputParser
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        NotificationHelper.createNotificationChannel(this)

        database = AppDatabase.getInstance(this)
        eventRepository = EventRepository(database.eventDao())
        predictionRepository = PredictionRepository(database.predictionDao())
        preferencesManager = PreferencesManager(this)
        secureKeyStorage = SecureKeyStorage(this)
        reminderAlarmManager = ReminderAlarmManager(this)

        geminiAiService = GeminiAIService()
        inputParser = InputParser(geminiAiService)
    }

    companion object {
        lateinit var instance: HydraApplication
            private set
    }
}
