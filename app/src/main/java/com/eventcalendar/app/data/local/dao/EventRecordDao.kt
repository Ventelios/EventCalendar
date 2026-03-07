package com.eventcalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eventcalendar.app.data.local.entity.EventRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface EventRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: EventRecord): Long

    @Delete
    suspend fun delete(record: EventRecord)

    @Query("DELETE FROM event_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM event_records WHERE id = :id")
    suspend fun getById(id: Long): EventRecord?

    @Query("SELECT * FROM event_records WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    fun getRecordsByDate(startTime: Long, endTime: Long): Flow<List<EventRecord>>

    @Query("SELECT * FROM event_records WHERE eventTypeId = :eventTypeId ORDER BY timestamp DESC")
    fun getRecordsByEventType(eventTypeId: Long): Flow<List<EventRecord>>

    @Query("SELECT * FROM event_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<EventRecord>>

    @Query("SELECT eventTypeId, COUNT(*) as count FROM event_records WHERE timestamp >= :startTime AND timestamp < :endTime GROUP BY eventTypeId ORDER BY count DESC")
    fun getEventFrequency(startTime: Long, endTime: Long): Flow<List<EventFrequencyRecord>>

    @Query("SELECT eventTypeId, COUNT(*) as count FROM event_records GROUP BY eventTypeId ORDER BY count DESC")
    fun getAllEventFrequency(): Flow<List<EventFrequencyRecord>>

    @Query("SELECT COUNT(*) FROM event_records WHERE eventTypeId = :eventTypeId")
    fun getCountByEventType(eventTypeId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM event_records WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getCountByTimeRange(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM event_records WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    suspend fun getRecordsByTimeRange(startTime: Long, endTime: Long): List<EventRecord>

    @Query("SELECT * FROM event_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsOnce(): List<EventRecord>

    @Query("DELETE FROM event_records")
    suspend fun deleteAll()
}

data class EventFrequencyRecord(
    val eventTypeId: Long,
    val count: Int
)
