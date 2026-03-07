package com.eventcalendar.app.data.model

import com.eventcalendar.app.data.local.entity.EventType
import com.eventcalendar.app.data.local.entity.EventRecord

data class AppBackupData(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val eventTypes: List<EventType>,
    val eventRecords: List<EventRecord>
)
