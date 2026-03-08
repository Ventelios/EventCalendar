package com.eventcalendar.app.data.repository

import com.eventcalendar.app.data.local.dao.EventRecordDao
import com.eventcalendar.app.data.local.dao.EventTypeDao
import com.eventcalendar.app.data.local.entity.EventRecord
import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.model.AppBackupData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class EventWithType(
    val record: EventRecord,
    val eventType: EventType
)

data class EventTypeWithCount(
    val eventType: EventType,
    val count: Int
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class EventRepository @Inject constructor(
    private val eventTypeDao: EventTypeDao,
    private val eventRecordDao: EventRecordDao
) {
    suspend fun insertEventType(eventType: EventType): Long {
        return eventTypeDao.insert(eventType)
    }

    suspend fun updateEventType(eventType: EventType) {
        eventTypeDao.update(eventType)
    }

    suspend fun deleteEventType(eventType: EventType) {
        eventTypeDao.delete(eventType)
    }

    suspend fun getEventTypeById(id: Long): EventType? {
        return eventTypeDao.getById(id)
    }

    fun getAllEventTypes(): Flow<List<EventType>> {
        return eventTypeDao.getAllEventTypes()
    }

    suspend fun getAllEventTypesOnce(): List<EventType> {
        return eventTypeDao.getAllEventTypesOnce()
    }

    suspend fun insertEventRecord(record: EventRecord): Long {
        return eventRecordDao.insert(record)
    }

    suspend fun updateEventRecord(record: EventRecord) {
        eventRecordDao.update(record)
    }

    suspend fun deleteEventRecord(record: EventRecord) {
        eventRecordDao.delete(record)
    }

    suspend fun deleteEventRecordById(id: Long) {
        eventRecordDao.deleteById(id)
    }

    suspend fun getEventRecordById(id: Long): EventRecord? {
        return eventRecordDao.getById(id)
    }

    fun getRecordsByDate(year: Int, month: Int, day: Int): Flow<List<EventWithType>> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return getRecordsWithType(eventRecordDao.getRecordsByDate(startOfDay, endOfDay))
    }

    fun getAllRecords(): Flow<List<EventWithType>> {
        return getRecordsWithType(eventRecordDao.getAllRecords())
    }

    fun getRecordsByEventType(eventTypeId: Long): Flow<List<EventWithType>> {
        return getRecordsWithType(eventRecordDao.getRecordsByEventType(eventTypeId))
    }

    fun getDatesWithEvents(year: Int, month: Int): Flow<Set<Int>> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return eventRecordDao.getRecordsByDate(startOfMonth, endOfMonth).mapLatest { records ->
            records.map { record ->
                Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    .get(Calendar.DAY_OF_MONTH)
            }.toSet()
        }
    }

    fun getEventFrequency(startTime: Long, endTime: Long): Flow<List<EventTypeWithCount>> {
        return eventRecordDao.getEventFrequency(startTime, endTime).mapLatest { frequencies ->
            frequencies.mapNotNull { freq ->
                val eventType = eventTypeDao.getById(freq.eventTypeId)
                eventType?.let { EventTypeWithCount(it, freq.count) }
            }
        }
    }

    fun getAllEventFrequency(): Flow<List<EventTypeWithCount>> {
        return eventRecordDao.getAllEventFrequency().mapLatest { frequencies ->
            frequencies.mapNotNull { freq ->
                val eventType = eventTypeDao.getById(freq.eventTypeId)
                eventType?.let { EventTypeWithCount(it, freq.count) }
            }
        }
    }

    suspend fun getEventCountByTimeRange(startTime: Long, endTime: Long): Int {
        return eventRecordDao.getCountByTimeRange(startTime, endTime)
    }

    suspend fun getRecordsByTimeRange(startTime: Long, endTime: Long): List<EventRecord> {
        return eventRecordDao.getRecordsByTimeRange(startTime, endTime)
    }

    private fun getRecordsWithType(recordsFlow: Flow<List<EventRecord>>): Flow<List<EventWithType>> {
        return recordsFlow.mapLatest { records ->
            records.mapNotNull { record ->
                val eventType = eventTypeDao.getById(record.eventTypeId)
                eventType?.let { EventWithType(record, it) }
            }
        }
    }

    suspend fun exportAllData(): AppBackupData {
        val eventTypes = eventTypeDao.getAllEventTypesOnce()
        val eventRecords = eventRecordDao.getAllRecordsOnce()
        return AppBackupData(
            eventTypes = eventTypes,
            eventRecords = eventRecords
        )
    }

    suspend fun importAllData(backupData: AppBackupData): ImportResult {
        try {
            eventTypeDao.deleteAll()
            eventRecordDao.deleteAll()

            val idMapping = mutableMapOf<Long, Long>()
            
            backupData.eventTypes.forEach { eventType ->
                val oldId = eventType.id
                val newId = eventTypeDao.insert(eventType.copy(id = 0))
                idMapping[oldId] = newId
            }

            backupData.eventRecords.forEach { record ->
                val newEventTypeId = idMapping[record.eventTypeId] ?: return ImportResult.Error("Invalid event type mapping")
                eventRecordDao.insert(record.copy(id = 0, eventTypeId = newEventTypeId))
            }

            return ImportResult.Success(
                eventTypesCount = backupData.eventTypes.size,
                eventRecordsCount = backupData.eventRecords.size
            )
        } catch (e: Exception) {
            return ImportResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class ImportResult {
    data class Success(val eventTypesCount: Int, val eventRecordsCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
