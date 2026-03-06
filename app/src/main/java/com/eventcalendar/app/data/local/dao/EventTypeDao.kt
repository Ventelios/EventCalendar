package com.eventcalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eventcalendar.app.data.local.entity.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventType: EventType): Long

    @Update
    suspend fun update(eventType: EventType)

    @Delete
    suspend fun delete(eventType: EventType)

    @Query("DELETE FROM event_types WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM event_types WHERE id = :id")
    suspend fun getById(id: Long): EventType?

    @Query("SELECT * FROM event_types ORDER BY name ASC")
    fun getAllEventTypes(): Flow<List<EventType>>

    @Query("SELECT * FROM event_types ORDER BY name ASC")
    suspend fun getAllEventTypesOnce(): List<EventType>

    @Query("SELECT COUNT(*) FROM event_types")
    fun getCount(): Flow<Int>
}
