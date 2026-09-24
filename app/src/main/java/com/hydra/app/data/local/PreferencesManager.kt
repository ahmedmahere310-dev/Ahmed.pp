package com.hydra.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hydra.app.data.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hydra_user_preferences")

data class HydraUserPreferences(
    val aiProvider: AiProvider = AiProvider.OFF,
    val remindersEnabled: Boolean = true,
    val minDelayMinutes: Int = 30,
    val maxDelayMinutes: Int = 120,
    val learningEnabled: Boolean = true,
    val dailyWaterGoalMl: Int = 2500,
    val snoozeMinutes: Int = 15,
    // Periodic Reminders
    val periodicWaterEnabled: Boolean = true,
    val waterIntervalMinutes: Int = 90,
    val periodicFoodEnabled: Boolean = true,
    val foodIntervalMinutes: Int = 240, // 4 hours
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 8, // 8 AM
    val lastWaterLoggedTime: Long = 0L,
    val lastFoodLoggedTime: Long = 0L
)

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val KEY_MIN_DELAY = intPreferencesKey("min_delay_minutes")
        val KEY_MAX_DELAY = intPreferencesKey("max_delay_minutes")
        val KEY_LEARNING_ENABLED = booleanPreferencesKey("learning_enabled")
        val KEY_DAILY_WATER_GOAL = intPreferencesKey("daily_water_goal_ml")
        val KEY_SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")

        // Periodic reminders keys
        val KEY_PERIODIC_WATER_ENABLED = booleanPreferencesKey("periodic_water_enabled")
        val KEY_WATER_INTERVAL_MINUTES = intPreferencesKey("water_interval_minutes")
        val KEY_PERIODIC_FOOD_ENABLED = booleanPreferencesKey("periodic_food_enabled")
        val KEY_FOOD_INTERVAL_MINUTES = intPreferencesKey("food_interval_minutes")
        val KEY_QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val KEY_QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val KEY_LAST_WATER_LOGGED = longPreferencesKey("last_water_logged_time")
        val KEY_LAST_FOOD_LOGGED = longPreferencesKey("last_food_logged_time")
    }

    val userPreferencesFlow: Flow<HydraUserPreferences> = context.dataStore.data.map { preferences ->
        val providerStr = preferences[KEY_AI_PROVIDER] ?: AiProvider.OFF.name
        val provider = try {
            AiProvider.valueOf(providerStr)
        } catch (e: Exception) {
            AiProvider.OFF
        }

        HydraUserPreferences(
            aiProvider = provider,
            remindersEnabled = preferences[KEY_REMINDERS_ENABLED] ?: true,
            minDelayMinutes = preferences[KEY_MIN_DELAY] ?: 30,
            maxDelayMinutes = preferences[KEY_MAX_DELAY] ?: 120,
            learningEnabled = preferences[KEY_LEARNING_ENABLED] ?: true,
            dailyWaterGoalMl = preferences[KEY_DAILY_WATER_GOAL] ?: 2500,
            snoozeMinutes = preferences[KEY_SNOOZE_MINUTES] ?: 15,
            periodicWaterEnabled = preferences[KEY_PERIODIC_WATER_ENABLED] ?: true,
            waterIntervalMinutes = preferences[KEY_WATER_INTERVAL_MINUTES] ?: 90,
            periodicFoodEnabled = preferences[KEY_PERIODIC_FOOD_ENABLED] ?: true,
            foodIntervalMinutes = preferences[KEY_FOOD_INTERVAL_MINUTES] ?: 240,
            quietHoursStart = preferences[KEY_QUIET_HOURS_START] ?: 22,
            quietHoursEnd = preferences[KEY_QUIET_HOURS_END] ?: 8,
            lastWaterLoggedTime = preferences[KEY_LAST_WATER_LOGGED] ?: 0L,
            lastFoodLoggedTime = preferences[KEY_LAST_FOOD_LOGGED] ?: 0L
        )
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_PROVIDER] = provider.name
        }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setDelayBounds(minMinutes: Int, maxMinutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MIN_DELAY] = minMinutes.coerceAtLeast(10)
            preferences[KEY_MAX_DELAY] = maxMinutes.coerceAtLeast(minMinutes + 5)
        }
    }

    suspend fun setLearningEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LEARNING_ENABLED] = enabled
        }
    }

    suspend fun setDailyWaterGoal(goalMl: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DAILY_WATER_GOAL] = goalMl
        }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SNOOZE_MINUTES] = minutes
        }
    }

    suspend fun setPeriodicWaterReminder(enabled: Boolean, intervalMinutes: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PERIODIC_WATER_ENABLED] = enabled
            if (intervalMinutes != null) {
                preferences[KEY_WATER_INTERVAL_MINUTES] = intervalMinutes.coerceIn(15, 360)
            }
        }
    }

    suspend fun setPeriodicFoodReminder(enabled: Boolean, intervalMinutes: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PERIODIC_FOOD_ENABLED] = enabled
            if (intervalMinutes != null) {
                preferences[KEY_FOOD_INTERVAL_MINUTES] = intervalMinutes.coerceIn(60, 480)
            }
        }
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_QUIET_HOURS_START] = startHour.coerceIn(0, 23)
            preferences[KEY_QUIET_HOURS_END] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun recordWaterLogged(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_WATER_LOGGED] = timestamp
        }
    }

    suspend fun recordFoodLogged(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_FOOD_LOGGED] = timestamp
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { it.clear() }
    }
}
