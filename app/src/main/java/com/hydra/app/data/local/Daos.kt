package com.hydra.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE createdAt >= :startOfDay AND createdAt <= :endOfDay ORDER BY createdAt DESC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE createdAt >= :startOfDay AND createdAt <= :endOfDay ORDER BY createdAt DESC")
    suspend fun getEventsForDayList(startOfDay: Long, endOfDay: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE type = 'WATER' AND createdAt >= :startOfDay AND createdAt <= :endOfDay ORDER BY createdAt DESC")
    fun getWaterToday(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE type = 'FOOD' AND createdAt >= :startOfDay AND createdAt <= :endOfDay ORDER BY createdAt DESC")
    fun getFoodToday(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE type = 'CREATINE' AND createdAt >= :startOfDay AND createdAt <= :endOfDay ORDER BY createdAt DESC")
    fun getCreatineToday(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE type = 'WATER' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestWaterEvent(): EventEntity?

    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    suspend fun getAllEventsList(): List<EventEntity>

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()
}

@Dao
interface PredictionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: PredictionSampleEntity): Long

    @Update
    suspend fun updateSample(sample: PredictionSampleEntity)

    @Query("SELECT * FROM prediction_samples WHERE feedbackGiven = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSampleWithoutFeedback(): PredictionSampleEntity?

    @Query("SELECT * FROM prediction_samples WHERE feedbackGiven = 1 ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentCompletedSamples(limit: Int = 20): Flow<List<PredictionSampleEntity>>

    @Query("SELECT * FROM prediction_samples WHERE feedbackGiven = 1 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentCompletedSamplesList(limit: Int = 20): List<PredictionSampleEntity>

    @Query("SELECT * FROM prediction_samples ORDER BY createdAt DESC")
    fun getAllSamples(): Flow<List<PredictionSampleEntity>>

    @Query("SELECT * FROM prediction_samples ORDER BY createdAt DESC")
    suspend fun getAllSamplesList(): List<PredictionSampleEntity>

    @Query("DELETE FROM prediction_samples")
    suspend fun clearAllSamples()
}
