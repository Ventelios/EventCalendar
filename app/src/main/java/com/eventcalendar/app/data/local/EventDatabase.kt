package com.eventcalendar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eventcalendar.app.data.local.dao.EventTypeDao
import com.eventcalendar.app.data.local.dao.EventRecordDao
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.local.entity.EventRecord

@Database(
    entities = [EventType::class, EventRecord::class],
    version = 1,
    exportSchema = false
)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventRecordDao(): EventRecordDao
}
