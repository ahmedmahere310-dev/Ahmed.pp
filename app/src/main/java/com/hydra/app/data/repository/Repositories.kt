package com.hydra.app.data.repository

import com.hydra.app.data.local.EventDao
import com.hydra.app.data.local.EventEntity
import com.hydra.app.data.local.PredictionDao
import com.hydra.app.data.local.PredictionSampleEntity
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {
    fun getEvents(): Flow<List<EventEntity>> = eventDao.getEvents()

    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>> =
        eventDao.getEventsForDay(startOfDay, endOfDay)

    suspend fun getEventsForDayList(startOfDay: Long, endOfDay: Long): List<EventEntity> =
        eventDao.getEventsForDayList(startOfDay, endOfDay)

    fun getWaterToday(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>> =
        eventDao.getWaterToday(startOfDay, endOfDay)

    fun getFoodToday(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>> =
        eventDao.getFoodToday(startOfDay, endOfDay)

    fun getCreatineToday(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>> =
        eventDao.getCreatineToday(startOfDay, endOfDay)

    suspend fun insertEvent(event: EventEntity): Long = eventDao.insertEvent(event)

    suspend fun updateEvent(event: EventEntity) = eventDao.updateEvent(event)

    suspend fun deleteEvent(event: EventEntity) = eventDao.deleteEvent(event)

    suspend fun deleteEventById(id: Long) = eventDao.deleteEventById(id)

    suspend fun getLatestWaterEvent(): EventEntity? = eventDao.getLatestWaterEvent()

    suspend fun getAllEventsList(): List<EventEntity> = eventDao.getAllEventsList()

    suspend fun clearAllEvents() = eventDao.clearAllEvents()
}

class PredictionRepository(private val predictionDao: PredictionDao) {
    fun getAllSamples(): Flow<List<PredictionSampleEntity>> = predictionDao.getAllSamples()

    fun getRecentCompletedSamples(limit: Int = 20): Flow<List<PredictionSampleEntity>> =
        predictionDao.getRecentCompletedSamples(limit)

    suspend fun getRecentCompletedSamplesList(limit: Int = 20): List<PredictionSampleEntity> =
        predictionDao.getRecentCompletedSamplesList(limit)

    suspend fun getLatestSampleWithoutFeedback(): PredictionSampleEntity? =
        predictionDao.getLatestSampleWithoutFeedback()

    suspend fun insertSample(sample: PredictionSampleEntity): Long =
        predictionDao.insertSample(sample)

    suspend fun updateSample(sample: PredictionSampleEntity) =
        predictionDao.updateSample(sample)

    suspend fun getAllSamplesList(): List<PredictionSampleEntity> =
        predictionDao.getAllSamplesList()

    suspend fun clearAllSamples() = predictionDao.clearAllSamples()
}
